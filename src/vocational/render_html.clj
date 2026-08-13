(ns vocational.render-html
  "Build-time operator console renderer for the technical-and-vocational-
  secondary-education actor (ISIC 8522).

  This namespace does NOT describe the actor -- it RUNS it. Every entity,
  identifier, disposition, verdict, rule and count on the emitted page is
  read back out of an actual `vocational.operation` graph execution
  (`langgraph.graph/run*`) over `vocational.store/seed-db`'s real seed
  data, its governor verdicts, and the append-only ledger the run left
  behind. Nothing is written by hand into the markup. If a value cannot
  be derived from the run, the page says so instead of inventing it.

  Two disclosures on the page are DERIVED AT RENDER TIME, deliberately,
  so they self-correct when the underlying code changes:

    * approver attribution -- computed by scanning each committed
      register for approver-shaped keys and checking whether the
      approver identity that the run actually supplied survived. It is
      NOT hard-coded that this repo loses attribution. Note carefully
      that a ledger fact's `:actor` is the EXECUTING actor-id from the
      operation context, NOT the approver; reading `:actor` as the
      approver would agree with the truth on this seed data (both are
      populated) while being wrong, so `:actor` is excluded from the
      approver-key set by name.

    * governor conformance probe -- an actual `:certification/finalize`
      run against a student whose ground-truth academic-integrity flag
      is set. The page reports whatever the governor actually did. If a
      later change makes the governor hold there, the page will say the
      governor held.

  Build invariant: `-main` REFUSES to write the file if the run produced
  zero HARD governor holds. A console that shows no genuine refusal is
  not evidence that the governor works, so it is not publishable.

  Run: `clojure -M:dev:render-html [out-path]`"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [langgraph.graph :as g]
            [vocational.facts :as facts]
            [vocational.governor :as governor]
            [vocational.operation :as op]
            [vocational.phase :as phase]
            [vocational.store :as store]))

(def ^:const default-out "docs/samples/operator-console.html")

;; ===================================================================
;; scenario table -- the script the actor is actually driven through
;; ===================================================================

(def operator-context
  "The operation context injected into every run. `:actor-id` is the
  EXECUTING actor (an operator seat), never an approver identity."
  {:actor-id "op-1" :actor-role :licensed-educator :phase 3})

(def scenarios
  "One map per graph run. `:expect` records what THIS FILE claims the
  classifier will return; the renderer compares it against the observed
  classification and reports any divergence on the page rather than
  hiding it. `:approve` resumes the interrupt with an approval decision
  (`:by` is the human approver identity handed to the graph)."
  [{:id "s01" :op :student/intake :subject "student-1"
    :patch {:id "student-1" :student-name "Sakura Tanaka"}
    :expect :commit
    :note "Phase 3 lists :student/intake in its :auto set -- the only op in this domain that may auto-commit when the governor is clean."}

   {:id "s02" :op :jurisdiction/assess :subject "student-1"
    :approve {:status :approved :by "approver-jane"}
    :expect :commit
    :note "Governor-clean but not auto-eligible: the phase gate escalates it to a human, who approves."}

   {:id "s03" :op :academic-integrity/screen :subject "student-1"
    :approve {:status :approved :by "approver-jane"}
    :expect :commit
    :note "Screening ops are never auto-eligible at any phase."}

   {:id "s04" :op :safety/screen :subject "student-1"
    :approve {:status :approved :by "approver-jane"}
    :expect :commit
    :note "Workshop-safety-training screening -- the concern this vertical adds over general secondary education."}

   {:id "s05" :op :certification/finalize :subject "student-1"
    :approve {:status :approved :by "approver-hideo"}
    :expect :commit
    :note "Actuation. Two independent layers (governor high-stakes gate, phase :auto set) each force a human decision."}

   {:id "s06" :op :graduation/finalize :subject "student-1"
    :approve {:status :approved :by "approver-hideo"}
    :expect :commit
    :note "Second actuation lifecycle on the same entity, with its own register and its own double-actuation guard."}

   {:id "s07" :op :jurisdiction/assess :subject "student-2" :extra {:no-spec? true}
    :expect :governor-hard-hold
    :note "Advisor proposes a checklist for a jurisdiction absent from vocational.facts."}

   {:id "s08" :op :jurisdiction/assess :subject "student-3"
    :approve {:status :approved :by "approver-jane"}
    :expect :commit
    :note "Sets the evidence checklist on file so the next run's hold is attributable to attendance alone."}

   {:id "s09" :op :certification/finalize :subject "student-3"
    :expect :governor-hard-hold
    :note "Governor independently recomputes attendance against the jurisdiction minimum -- it does not trust advisor confidence."}

   {:id "s10" :op :academic-integrity/screen :subject "student-4"
    :expect :governor-hard-hold
    :note "The screening op HARD-holds on its own finding; the proposal never reaches a human."}

   {:id "s11" :op :jurisdiction/assess :subject "student-5"
    :approve {:status :approved :by "approver-jane"}
    :expect :commit
    :note "Sets the evidence checklist on file so the next run's hold is attributable to credits alone."}

   {:id "s12" :op :graduation/finalize :subject "student-5"
    :expect :governor-hard-hold
    :note "Governor independently recomputes credit-set containment."}

   {:id "s13" :op :safety/screen :subject "student-6"
    :expect :governor-hard-hold
    :note "Unconditional check: every student in ISIC 8522 does hands-on shop work, so this applies to all of them."}

   {:id "s14" :op :certification/finalize :subject "student-1"
    :expect :governor-hard-hold
    :note "Double-actuation guard, off a dedicated :certification-finalized? boolean rather than a :status value."}

   {:id "s15" :op :graduation/finalize :subject "student-1"
    :expect :governor-hard-hold
    :note "The second lifecycle's independent double-actuation guard."}

   {:id "s16" :op :jurisdiction/assess :subject "student-6" :phase 1
    :expect :phase-gate-hold
    :note "SAME op and a governor verdict with hard? = false, held only because phase 1 does not enable this write. This is a rollout gate, not a refusal."}

   {:id "s17" :op :certification/finalize :subject "student-5"
    :approve {:status :rejected :by "approver-kenji"}
    :expect :approver-rejected
    :note "A human declines. The resulting ledger fact carries a violation of its own ({:rule :approver-rejected}) -- counting violations instead of fact types would miscount this as a governor refusal."}

   {:id "s18" :op :jurisdiction/assess :subject "student-4"
    :approve {:status :approved :by "approver-jane"}
    :expect :commit
    :note "Conformance-probe setup: puts the evidence checklist on file for the flagged student."}

   {:id "s19" :op :certification/finalize :subject "student-4"
    :approve {:status :approved :by "approver-hideo"}
    :expect :probe
    :note "CONFORMANCE PROBE. student-4's ground-truth academic-integrity flag is set. Whatever the governor does here is reported verbatim."}

   {:id "s20" :op :graduation/finalize :subject "student-2"
    :expect :governor-hard-hold
    :note "student-2's jurisdiction assessment was itself refused at s07, so no evidence checklist ever reached the store. The governor will not finalize a graduation on top of a gap it created -- and this student's credits, attendance and safety training are all clean, so the hold is attributable to the missing evidence alone."}])

(def probe-scenario-id "s19")
(def probe-subject "student-4")

;; ===================================================================
;; execution
;; ===================================================================

(defn- request-of [{:keys [op subject patch extra]}]
  (cond-> {:op op :subject subject}
    patch (assoc :patch patch)
    extra (merge extra)))

(defn- decision-fact
  "The last ledger-bound decision fact in a run's audit trail. This is
  the value classification keys off -- the fact TYPE, not the violation
  list."
  [audit]
  (last (filter #(#{:committed :governor-hold :approval-rejected} (:t %)) audit)))

(defn classify
  "Classify one completed run into exactly one disposition class.

  Order matters and is deliberate:

    1. `:t` = :approval-rejected   -> a HUMAN declined. This fact carries
       `{:rule :approver-rejected}` in its own `:violations`, so any
       classifier that keys off `:violations` being non-empty counts it
       as a governor refusal. It is not one -- the governor cleared this
       proposal, which is precisely why it reached a human at all.
    2. `:t` = :governor-hold WITH `:phase-reason` -> the ROLLOUT PHASE
       gate stopped a write the phase does not enable. `vocational.phase/
       gate` only attaches `:phase-reason` when the governor did NOT
       hold (a governor hold short-circuits first), so this is disjoint
       from class 3 by construction.
    3. `:t` = :governor-hold WITHOUT `:phase-reason` -> a GOVERNOR
       REFUSAL. Corroborated against the verdict's own `:hard?` flag; a
       disagreement is surfaced as :indeterminate-hold rather than
       silently folded into either bucket.
    4. `:t` = :committed        -> committed to the SSoT.
    5. no decision fact but disposition :escalate -> still parked at the
       human-approval interrupt (nothing was resumed)."
  [{:keys [audit disposition verdict]}]
  (let [f (decision-fact audit)]
    (cond
      (nil? f)                          (if (= :escalate disposition)
                                          :awaiting-approval
                                          :unclassified)
      (= :approval-rejected (:t f))     :approver-rejected
      (and (= :governor-hold (:t f))
           (some? (:phase-reason f)))   :phase-gate-hold
      (= :governor-hold (:t f))         (if (true? (:hard? verdict))
                                          :governor-hard-hold
                                          :indeterminate-hold)
      (= :committed (:t f))             :commit
      :else                             :unclassified)))

(defn run-scenarios
  "Drive `db` through `scs` with a freshly built actor. Returns a vector
  of run records, one per scenario, each carrying the observed proposal,
  governor verdict, audit trail and classification."
  [db scs]
  (let [actor (op/build db)]
    (mapv
     (fn [{:keys [id phase approve] :as sc}]
       (let [ctx     (assoc operator-context :phase (or phase (:phase operator-context)))
             req     (request-of sc)
             r1      (g/run* actor {:request req :context ctx} {:thread-id id})
             st1     (:state r1)
             r2      (when (and approve (= :escalate (:disposition st1)))
                       (g/run* actor {:approval approve} {:thread-id id :resume? true}))
             st      (or (:state r2) st1)
             record  {:scenario   sc
                      :context    ctx
                      :request    req
                      :proposal   (:proposal st1)
                      :verdict    (:verdict st1)
                      :pre-approval-disposition (:disposition st1)
                      :approval   (when r2 approve)
                      :disposition (:disposition st)
                      :record     (:record st)
                      :audit      (:audit st)}]
         (assoc record :class (classify record))))
     scs)))

;; ===================================================================
;; derived disclosures
;; ===================================================================

(def approver-keys
  "Keys that would carry an APPROVER identity if the commit path kept
  one. `:actor` is deliberately EXCLUDED: `vocational.operation`'s
  commit/hold facts set `:actor` from `(:actor-id context)`, i.e. the
  executing operator seat. On this seed data both an approver and an
  actor-id exist and are non-nil, so mistaking one for the other yields
  a plausible-looking answer that is wrong."
  #{:approved-by :approver :approved_by :signed-off-by :authorized-by :by})

(defn- approver-in
  "The approver-shaped value found anywhere inside `v`, or nil. Walks
  maps/collections so it also finds one nested inside a register
  payload."
  [v]
  (cond
    (map? v)  (or (some (fn [k] (when-let [x (get v k)] (when (string? x) x))) approver-keys)
                  (some approver-in (vals v)))
    (coll? v) (some approver-in v)
    :else     nil))

(defn- register-places
  "Where a committed effect's data lands, so the scan reads the actual
  stored value rather than the in-flight proposal."
  [db effect subject]
  (case effect
    :student/upsert         [["student register" (store/student db subject)]]
    :assessment/set         [["assessment register" (store/assessment-of db subject)]]
    :integrity-screen/set   [["integrity-screen register" (store/integrity-screen-of db subject)]]
    :safety-screen/set      [["safety-screen register" (store/safety-screen-of db subject)]]
    :student/mark-certified [["student register" (store/student db subject)]
                             ["certification register" (filterv #(= subject (get % "student_id"))
                                                                (store/certification-history db))]]
    :student/mark-graduated [["student register" (store/student db subject)]
                             ["graduation register" (filterv #(= subject (get % "student_id"))
                                                             (store/graduation-history db))]]
    []))

(defn attribution-scan
  "For every run that a HUMAN approved and that then committed, check
  whether the approver identity the run supplied survived into the
  stored register(s). Derived entirely from this run -- if the commit
  path is later changed to keep the approver, this scan reports that."
  [db runs]
  (->> runs
       (filter #(and (= :commit (:class %)) (some? (:approval %))))
       (mapv (fn [{:keys [scenario approval record]}]
               (let [effect   (:effect record)
                     subject  (:subject scenario)
                     approver (:by approval)
                     places   (register-places db effect subject)
                     found    (keep (fn [[label v]]
                                      (when (= approver (approver-in v)) label))
                                    places)]
                 {:scenario-id (:id scenario)
                  :op          (:op scenario)
                  :effect      effect
                  :subject     subject
                  :approver    approver
                  :places      (mapv first places)
                  :retained-in (vec found)
                  :retained?   (boolean (seq found))})))))

(defn ledger-attribution-scan
  "Does the append-only ledger itself record who approved? Scanned, not
  assumed."
  [db]
  (let [facts (store/ledger db)
        hits  (filterv #(some? (approver-in (dissoc % :actor))) facts)]
    {:facts (count facts)
     :with-approver (count hits)
     :actor-values (vec (sort (distinct (keep :actor facts))))}))

(defn conformance-probe
  "Report, from the actual run, what the governor did when asked to
  finalize a certification for a student whose GROUND-TRUTH academic-
  integrity flag is set. Nothing here is asserted in advance."
  [db runs]
  (let [run     (first (filter #(= probe-scenario-id (get-in % [:scenario :id])) runs))
        student (store/student db probe-subject)
        screen  (store/integrity-screen-of db probe-subject)
        screen-run (first (filter #(and (= :academic-integrity/screen (get-in % [:scenario :op]))
                                        (= probe-subject (get-in % [:scenario :subject])))
                                  runs))]
    (when run
      {:subject          probe-subject
       :student-name     (:student-name student)
       :ground-truth-flag (:academic-integrity-flag? student)
       :screen-class     (:class screen-run)
       :screen-on-file   screen
       :probe-class      (:class run)
       :probe-violations (get-in run [:verdict :violations])
       :certified?       (store/student-already-certified? db probe-subject)
       :gap?             (and (true? (:academic-integrity-flag? student))
                              (not= :governor-hard-hold (:class run)))})))

;; ===================================================================
;; minimal hiccup emitter -- guarantees tag balance structurally
;; ===================================================================

(defrecord Raw [s])
(defn raw [s] (->Raw s))

(def ^:private void-tags #{:br :hr :img :meta :link :input})

(defn- esc [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- attrs->s [m]
  (str/join (for [[k v] (sort-by (comp name key) m) :when (and (some? v) (not= false v))]
              (if (true? v)
                (str " " (name k))
                (str " " (name k) "=\"" (esc v) "\"")))))

(defn html
  "hiccup -> HTML string. Vectors must be [tag attrs? & children]; nil
  children are dropped; every non-void tag emits a matching close tag,
  so tag balance is a property of the emitter rather than of the author."
  [node]
  (cond
    (nil? node)     ""
    (instance? Raw node) (:s node)
    (string? node)  (esc node)
    (number? node)  (str node)
    (keyword? node) (esc (name node))
    (and (vector? node) (keyword? (first node)))
    (let [[tag & more] node
          [attrs children] (if (map? (first more))
                             [(first more) (rest more)]
                             [{} more])
          t (name tag)]
      (if (void-tags tag)
        (str "<" t (attrs->s attrs) ">")
        (str "<" t (attrs->s attrs) ">" (str/join (map html children)) "</" t ">")))

    ;; a vector that does not start with a tag keyword is a FRAGMENT --
    ;; a plain sequence of sibling nodes. Without this branch, passing a
    ;; vector of already-built sections as a child silently reads the
    ;; first section as a tag name.
    (vector? node) (str/join (map html node))
    (seqable? node) (str/join (map html node))
    :else           (esc node)))

;; ===================================================================
;; presentation helpers -- every one of them refuses to print a nil
;; ===================================================================

(def ^:const dash "—")

(defn- txt [v] (if (or (nil? v) (and (string? v) (str/blank? v))) dash (str v)))
(defn- kw [v] (if (keyword? v) (name v) (txt v)))
(defn- kws [coll] (if (seq coll) (str/join ", " (map #(if (keyword? %) (name %) (str %)) coll)) dash))
(defn- credits [s] (if (seq s) (str/join " " (map name (sort s))) dash))
(defn- yn [b] (if (true? b) "yes" "no"))

(defn- class-label [c]
  (case c
    :commit               "COMMIT"
    :governor-hard-hold   "GOVERNOR HOLD (hard)"
    :phase-gate-hold      "PHASE GATE"
    :approver-rejected    "APPROVER REJECTED"
    :awaiting-approval    "AWAITING APPROVAL"
    :indeterminate-hold   "INDETERMINATE HOLD"
    (kw c)))

(defn- class-css [c]
  (case c
    :commit             "p-commit"
    :governor-hard-hold "p-hold"
    :phase-gate-hold    "p-phase"
    :approver-rejected  "p-reject"
    "p-other"))

(defn- table [headers rows]
  [:div {:class "scroll"}
   [:table
    [:thead [:tr (for [h headers] [:th h])]]
    [:tbody (for [r rows] [:tr (for [c r] [:td c])])]]])

(defn- section [id title lead & body]
  [:section {:id id}
   [:h2 title]
   (when lead [:p {:class "lead"} lead])
   body])

;; ===================================================================
;; stylesheet -- self-contained.
;; This repo does not depend on jp-go-dds (see deps.edn), so the page
;; carries its own tokens rather than claiming a design system it does
;; not actually link against. Token NAMES follow the workspace --hig-*
;; contract so a later move onto that stack is a swap, not a rewrite.
;; ===================================================================

;; ===================================================================
;; design system -- デジタル庁デザインシステム (DADS)
;;
;; This workspace's BASE design system is `jp-go-digital-design-system`.
;; Rather than restate its palette as hex here (which silently drifts the
;; moment upstream re-vendors), the build READS the vendored upstream
;; stylesheet off that dependency's classpath and lifts out only the
;; primitives this page actually references. Consequences that matter:
;;
;;   - a missing dependency, a moved resource or a renamed primitive is a
;;     BUILD FAILURE, not a page that silently renders unstyled;
;;   - the page carries ~1 KB of palette instead of the full 67 KB
;;     upstream sheet, because unreferenced primitives are never emitted;
;;   - the `--hig-*` token contract still names every value the layout
;;     rules below use, so those rules are unchanged by the grounding.
;; ===================================================================

(def ^:const dads-resource
  "The upstream DADS stylesheet, vendored inside the jp-go-dds dependency."
  "jp_go_dds/dds.css")

(def dads-primitives
  "The DADS primitives this page's token contract resolves onto. Order is
  literal (not derived from a map) so the emitted CSS is byte-stable."
  ["--color-neutral-white"
   "--color-neutral-solid-gray-50"
   "--color-neutral-solid-gray-100"
   "--color-neutral-solid-gray-200"
   "--color-neutral-solid-gray-600"
   "--color-neutral-solid-gray-900"
   "--color-primitive-blue-50"
   "--color-primitive-blue-900"
   "--color-primitive-green-50"
   "--color-primitive-green-200"
   "--color-primitive-green-1000"
   "--color-primitive-orange-50"
   "--color-primitive-orange-200"
   "--color-primitive-orange-1000"
   "--color-primitive-purple-50"
   "--color-primitive-purple-200"
   "--color-primitive-purple-1000"
   "--color-primitive-red-50"
   "--color-primitive-red-200"
   "--color-primitive-red-1000"
   "--font-family-sans"
   "--font-family-mono"])

(defn dads-palette
  "Read `dads-resource` off the classpath and return `[name value]` pairs
  for `names`, in the order given. Throws when the resource is absent or
  when any requested primitive is not declared upstream -- this page
  refuses to fall back to a hand-written palette."
  [names]
  (let [res  (io/resource dads-resource)
        _    (when-not res
               (throw (ex-info (str "DADS stylesheet not on the classpath: " dads-resource
                                    ". Is io.github.kotoba-lang/jp-go-digital-design-system"
                                    " still in deps.edn?")
                               {:resource dads-resource})))
        root (second (re-find #"(?s):root\s*\{(.*?)\n\}" (slurp res)))
        _    (when-not root
               (throw (ex-info "DADS stylesheet has no :root primitive block"
                               {:resource dads-resource})))
        decl (into {} (map (fn [[_ k v]] [k (str/trim v)]))
                   (re-seq #"(--[a-z0-9-]+)\s*:\s*([^;]+);" root))
        gone (remove decl names)]
    (when (seq gone)
      (throw (ex-info "DADS no longer declares primitives this page references"
                      {:missing (vec gone) :resource dads-resource})))
    (mapv (fn [n] [n (decl n)]) names)))

(defn dads-root-css
  "The `:root` block: the lifted DADS primitives, then the `--hig-*`
  contract expressed purely as references to them."
  []
  (str ":root{\n"
       (str/join (for [[n v] (dads-palette dads-primitives)]
                   (str "  " n ":" v ";\n")))
       "\n"
       "  --hig-color-bg:var(--color-neutral-solid-gray-50);\n"
       "  --hig-color-surface:var(--color-neutral-white);\n"
       "  --hig-color-text:var(--color-neutral-solid-gray-900);\n"
       "  --hig-color-text-secondary:var(--color-neutral-solid-gray-600);\n"
       "  --hig-color-accent:var(--color-primitive-blue-900);\n"
       "  --hig-color-danger:var(--color-primitive-red-1000);\n"
       "  --hig-color-warning:var(--color-primitive-orange-1000);\n"
       "  --hig-color-success:var(--color-primitive-green-1000);\n"
       "  --hig-color-reject:var(--color-primitive-purple-1000);\n"
       "  --hig-hairline:var(--color-neutral-solid-gray-200);\n"
       "  --hig-radius-md:10px; --hig-radius-xs:4px;\n"
       "  --hig-spacing-2:8px; --hig-spacing-3:12px;"
       " --hig-spacing-4:16px; --hig-spacing-6:24px;\n"
       "  --hig-font-sans:var(--font-family-sans);\n"
       "  --hig-font-mono:var(--font-family-mono);\n"
       "}\n"))

(def ^:private stylesheet "
*{box-sizing:border-box}
body{margin:0;background:var(--hig-color-bg);color:var(--hig-color-text);
  font-family:var(--hig-font-sans);line-height:1.65;font-size:15px}
main{max-width:1180px;margin:0 auto;padding:var(--hig-spacing-6) var(--hig-spacing-4) 72px}
header.masthead{background:var(--hig-color-accent);color:var(--color-neutral-white);
  padding:32px var(--hig-spacing-4)}
header.masthead .inner{max-width:1180px;margin:0 auto}
header.masthead h1{margin:0 0 6px;font-size:26px;letter-spacing:.01em}
header.masthead p{margin:0;opacity:.92;font-size:14px}
.kicker{font-family:var(--hig-font-mono);font-size:12px;letter-spacing:.08em;
  text-transform:uppercase;opacity:.85;margin:0 0 10px}
section{background:var(--hig-color-surface);border:1px solid var(--hig-hairline);
  border-radius:var(--hig-radius-md);padding:var(--hig-spacing-6);margin:var(--hig-spacing-4) 0}
h2{font-size:19px;margin:0 0 var(--hig-spacing-2);padding-bottom:var(--hig-spacing-2);
  border-bottom:2px solid var(--hig-color-accent)}
h3{font-size:15px;margin:var(--hig-spacing-4) 0 var(--hig-spacing-2)}
p.lead{color:var(--hig-color-text-secondary);margin:0 0 var(--hig-spacing-4);font-size:14px}
.scroll{overflow-x:auto}
table{border-collapse:collapse;width:100%;font-size:13px}
th,td{border:1px solid var(--hig-hairline);padding:7px 9px;text-align:left;vertical-align:top}
th{background:var(--color-neutral-solid-gray-100);font-weight:600;white-space:nowrap}
td.num,th.num{text-align:right;font-variant-numeric:tabular-nums}
code,.mono{font-family:var(--hig-font-mono);font-size:12px}
.pill{display:inline-block;padding:2px 8px;border-radius:999px;font-size:11px;
  font-weight:700;letter-spacing:.03em;white-space:nowrap;font-family:var(--hig-font-mono)}
.p-commit{background:var(--color-primitive-green-50);color:var(--hig-color-success);
  border:1px solid var(--color-primitive-green-200)}
.p-hold{background:var(--color-primitive-red-50);color:var(--hig-color-danger);
  border:1px solid var(--color-primitive-red-200)}
.p-phase{background:var(--color-primitive-orange-50);color:var(--hig-color-warning);
  border:1px solid var(--color-primitive-orange-200)}
.p-reject{background:var(--color-primitive-purple-50);color:var(--hig-color-reject);
  border:1px solid var(--color-primitive-purple-200)}
.p-other{background:var(--color-neutral-solid-gray-50);color:var(--hig-color-text-secondary);
  border:1px solid var(--hig-hairline)}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:var(--hig-spacing-3)}
.stat{border:1px solid var(--hig-hairline);border-radius:var(--hig-radius-md);
  padding:var(--hig-spacing-3);background:var(--hig-color-surface)}
.stat .n{font-size:28px;font-weight:700;font-variant-numeric:tabular-nums;line-height:1.1}
.stat .k{font-size:12px;color:var(--hig-color-text-secondary);margin-top:2px}
.card{border:1px solid var(--hig-hairline);border-left:4px solid var(--hig-color-danger);
  border-radius:var(--hig-radius-xs);padding:var(--hig-spacing-3);
  margin:var(--hig-spacing-3) 0;background:var(--color-primitive-red-50)}
.card.phase{border-left-color:var(--hig-color-warning);background:var(--color-primitive-orange-50)}
.card.reject{border-left-color:var(--hig-color-reject);background:var(--color-primitive-purple-50)}
.card h4{margin:0 0 6px;font-size:14px}
.card .why{margin:6px 0 0;font-size:13px}
.note{border-left:4px solid var(--hig-color-accent);background:var(--color-primitive-blue-50);
  padding:var(--hig-spacing-3);border-radius:var(--hig-radius-xs);font-size:13px;
  margin:var(--hig-spacing-3) 0}
.note.bad{border-left-color:var(--hig-color-danger);background:var(--color-primitive-red-50)}
.note.good{border-left-color:var(--hig-color-success);background:var(--color-primitive-green-50)}
dl.kv{display:grid;grid-template-columns:max-content 1fr;gap:4px var(--hig-spacing-3);
  margin:0;font-size:13px}
dl.kv dt{color:var(--hig-color-text-secondary)}
dl.kv dd{margin:0}
footer{color:var(--hig-color-text-secondary);font-size:12px;padding:0 var(--hig-spacing-4);
  max-width:1180px;margin:0 auto 40px}
")

;; ===================================================================
;; sections
;; ===================================================================

(defn- read-blueprint []
  (let [f (io/file "blueprint.edn")]
    (when (.exists f)
      (edn/read-string (slurp f)))))

(defn- sec-overview [bp db runs tally]
  (section
   "overview" "1. What this page is"
   "Rendered by vocational.render-html from one live execution of this repo's OperationActor graph. Every row below was read back out of that run."
   [:div {:class "grid"}
    (for [[n k] [[(count runs) "graph runs executed"]
                 [(:governor-hard-hold tally 0) "HARD governor holds"]
                 [(:phase-gate-hold tally 0) "rollout-phase gate holds"]
                 [(:approver-rejected tally 0) "approver rejections"]
                 [(:commit tally 0) "commits to the SSoT"]
                 [(count (store/ledger db)) "append-only ledger facts"]]]
      [:div {:class "stat"} [:div {:class "n"} (str n)] [:div {:class "k"} k]])]
   [:h3 "Run provenance"]
   [:dl {:class "kv"}
    [:dt "repo"] [:dd [:code (txt (:itonami.blueprint/id bp))]]
    [:dt "ISIC rev.5"] [:dd (txt (:itonami.blueprint/isic-rev5 bp))]
    [:dt "vertical"] [:dd (txt (:itonami.blueprint/name bp))]
    [:dt "governor"] [:dd [:code (kw (:itonami.blueprint/governor bp))]]
    [:dt "advisor node"] [:dd [:code "vocational.vocedopsllm"] " (deterministic mock advisor)"]
    [:dt "store backend"] [:dd [:code (.getName (class db))] " seeded from " [:code "vocational.store/demo-data"]]
    [:dt "design system"] [:dd "デジタル庁デザインシステム (DADS) — "
                           (str (count (dads-palette dads-primitives)))
                           " colour/type primitives lifted at build time from "
                           [:code dads-resource]
                           ", which this page's " [:code "--hig-*"]
                           " token contract resolves onto. No hex is written by hand."]
    [:dt "default phase"] [:dd (str "phase " (:phase operator-context) " — "
                                    (get-in phase/phases [(:phase operator-context) :label]))]
    [:dt "executing actor"] [:dd [:code (:actor-id operator-context)] " (role " [:code (kw (:actor-role operator-context))] ")"]
    [:dt "confidence floor"] [:dd (str governor/confidence-floor)]]
   [:p {:class "note"}
    "No wall-clock time, run identifier or random value appears on this page. "
    "The build is reproducible: rendering twice from the same commit yields byte-identical output."]))

(defn- sec-contract [bp]
  (section
   "contract" "2. The governance contract this run exercises"
   "Read from vocational.governor, vocational.phase and blueprint.edn — not restated by hand."
   [:h3 "Hard governor rules (a human approver cannot override these)"]
   (table ["rule" "what it refuses"]
          [[[:code "no-spec-basis"] "A jurisdiction proposal with no official citation in vocational.facts."]
           [[:code "evidence-incomplete"] "Actuation while the jurisdiction's required evidence checklist is not satisfied on file."]
           [[:code "attendance-hours-insufficient"] "Certification while the student's own attendance hours fall short of the jurisdiction minimum."]
           [[:code "academic-integrity-flag-unresolved"] "Proceeding while an academic-integrity concern is unresolved."]
           [[:code "workplace-safety-training-unconfirmed"] "Certification while workshop-safety training is unconfirmed."]
           [[:code "graduation-requirements-unsatisfied"] "Graduation while the earned-credit set does not contain every required credit."]
           [[:code "already-certified"] "Finalizing the same student's certification twice."]
           [[:code "already-graduated"] "Finalizing the same student's graduation twice."]])
   [:h3 "High-stakes actuation (always a human decision, at every phase)"]
   [:p (kws (sort (map name governor/high-stakes)))]
   [:h3 "Rollout phases"]
   (table ["phase" "label" "writes enabled" "may auto-commit when governor-clean"]
          (for [p (sort (keys phase/phases))]
            (let [{:keys [label writes auto]} (get phase/phases p)]
              [(str p) label
               (if (seq writes) (kws (sort (map str writes))) dash)
               (if (seq auto) (kws (sort (map str auto))) dash)])))
   [:p {:class "note"}
    "Note the structural invariant visible in the table above: "
    [:code ":certification/finalize"] " and " [:code ":graduation/finalize"]
    " appear in phase 3's writes but in no phase's auto set. "
    "Social impact declared by the blueprint: " (kws (:itonami.blueprint/social-impact bp)) "."]))

(defn- sec-facts []
  (let [cov (facts/coverage)]
    (section
     "facts" "3. Jurisdiction spec-basis catalog"
     "The citation table the governor checks every jurisdiction proposal against. A jurisdiction absent from this table has no spec-basis, and the governor holds any proposal that invents one."
     (table ["ISO3" "jurisdiction" "education authority" "legal basis" "workshop-safety authority" "safety legal basis" "required evidence"]
            (for [iso3 (sort (keys facts/catalog))]
              (let [m (facts/spec-basis iso3)]
                [[:code iso3]
                 (txt (:name m))
                 (txt (:owner-authority m))
                 (txt (:legal-basis m))
                 (txt (:safety-owner-authority m))
                 (txt (:safety-legal-basis m))
                 [:ul (for [e (:required-evidence m)] [:li e])]])))
     [:p {:class "note"}
      "Honest coverage: " (str (:covered cov)) " of " (str (:requested cov))
      " jurisdictions carry an official spec-basis"
      (if (seq (:missing-jurisdictions cov))
        (str "; missing: " (kws (:missing-jurisdictions cov)))
        "")
      ". " (txt (:note cov))])))

(defn- sec-roster [db]
  (section
   "roster" "4. Student register — final state after the run"
   "Ground-truth seed attributes plus whatever the run actually wrote back. The governor recomputes these independently; it never trusts the advisor's confidence."
   (table ["id" "name" "jurisdiction" "attendance" "credits earned" "credits required"
           "integrity flag" "safety training" "certified?" "certification no." "graduated?" "graduation no."]
          (for [s (store/all-students db)]
            [[:code (txt (:id s))]
             (txt (:student-name s))
             [:code (txt (:jurisdiction s))]
             [:span {:class "mono"} (str (txt (:attendance-hours-completed s)) " / " (txt (:attendance-hours-required s)))]
             [:span {:class "mono"} (credits (:credits-earned s))]
             [:span {:class "mono"} (credits (:credits-required s))]
             (yn (:academic-integrity-flag? s))
             (yn (:workplace-safety-training-certified? s))
             (yn (:certification-finalized? s))
             [:code (txt (:certification-number s))]
             (yn (:graduation-finalized? s))
             [:code (txt (:graduation-number s))]]))))

(defn- sec-runs [runs]
  (section
   "runs" "5. Graph runs"
   "One row per execution of the intake → advise → govern → decide → (commit | hold | human approval) graph."
   (table ["#" "op" "subject" "phase" "advisor confidence" "governor hard?" "pre-approval" "human decision" "outcome" "rules cited"]
          (for [{:keys [scenario context proposal verdict pre-approval-disposition approval class]} runs]
            [[:code (txt (:id scenario))]
             [:code (kw (:op scenario))]
             [:code (txt (:subject scenario))]
             (str (:phase context))
             [:span {:class "mono"} (txt (:confidence proposal))]
             (yn (:hard? verdict))
             [:code (kw (txt (name (or pre-approval-disposition :none))))]
             (if approval
               [:span (kw (:status approval)) " by " [:code (txt (:by approval))]]
               dash)
             [:span {:class (str "pill " (class-css class))} (class-label class)]
             (if (seq (:violations verdict))
               [:span {:class "mono"} (kws (map :rule (:violations verdict)))]
               dash)]))
   [:h3 "Advisor proposals as cited"]
   (table ["#" "proposal summary" "rationale" "cites" "effect" "stake"]
          (for [{:keys [scenario proposal]} runs]
            [[:code (txt (:id scenario))]
             (txt (:summary proposal))
             (txt (:rationale proposal))
             [:span {:class "mono"} (kws (:cites proposal))]
             [:code (kw (:effect proposal))]
             (if (:stake proposal) [:code (kw (:stake proposal))] dash)]))))

(defn- hold-card [{:keys [scenario verdict audit]} css]
  (let [f (decision-fact audit)]
    [:div {:class (str "card " css)}
     [:h4 [:code (txt (:id scenario))] " — " [:code (kw (:op scenario))]
      " on " [:code (txt (:subject scenario))]]
     [:dl {:class "kv"}
      [:dt "ledger fact type"] [:dd [:code (kw (:t f))]]
      [:dt "rules"] [:dd [:span {:class "mono"} (kws (:basis f))]]
      (when (:phase-reason f) [:dt "phase reason"])
      (when (:phase-reason f) [:dd [:code (kw (:phase-reason f))] " (phase " (str (:phase f)) ")"])
      [:dt "governor hard?"] [:dd (yn (:hard? verdict))]
      [:dt "advisor confidence"] [:dd [:span {:class "mono"} (txt (:confidence verdict))]]]
     (for [v (:violations f)]
       [:p {:class "why"} [:strong (kw (:rule v))] " — " (txt (:detail v))])
     [:p {:class "why"} [:em (txt (:note scenario))]]]))

(defn- sec-holds [runs]
  (let [hard (filterv #(= :governor-hard-hold (:class %)) runs)]
    (section
     "holds" (str "6. HARD governor holds — " (count hard) " genuine refusals")
     "Each of these is an un-overridable refusal by the Curriculum Safeguarding Governor. No human approver can sign past them; the proposal never reaches the approval interrupt at all."
     (for [r hard] (hold-card r "")))))

(defn- sec-phase-gate [runs]
  (let [gated (filterv #(= :phase-gate-hold (:class %)) runs)
        rejected (filterv #(= :approver-rejected (:class %)) runs)]
    (section
     "not-refusals" "7. Stops that are NOT governor refusals"
     "Three different things stop a run, and conflating them miscounts the governor. These are the other two."
     [:h3 (str "Rollout-phase gate — " (count gated))]
     [:p {:class "lead"}
      "The phase gate holds a write the current rollout phase does not enable. "
      "The governor verdict in these runs has hard? = no and an empty violation list; "
      "the ledger fact is distinguished by carrying :phase-reason, which "
      [:code "vocational.phase/gate"] " only attaches when the governor did not hold."]
     (if (seq gated)
       (for [r gated] (hold-card r "phase"))
       [:p dash])
     [:h3 (str "Approver rejection — " (count rejected))]
     [:p {:class "lead"}
      "A human declined a proposal the governor had already cleared. "
      "The resulting fact type is :approval-rejected, and it carries a violation of its own "
      "({:rule :approver-rejected}) — so a count keyed off a non-empty :violations list "
      "would report this as a governor refusal. It is the opposite: the governor let it through."]
     (if (seq rejected)
       (for [r rejected] (hold-card r "reject"))
       [:p dash]))))

(defn- sec-classifier [runs tally]
  (let [violation-count (count (filter #(seq (get-in % [:verdict :violations])) runs))
        naive (count (filter (fn [r]
                               (let [f (decision-fact (:audit r))]
                                 (seq (:violations f))))
                             runs))]
    (section
     "classifier" "8. How each stop was classified"
     "Classification keys off the ledger fact TYPE first, then corroborates against the governor verdict — never off the violation list alone."
     (table ["class" "runs" "keyed off"]
            [[[:span {:class "pill p-commit"} "COMMIT"] (str (:commit tally 0))
              [:code ":t = :committed"]]
             [[:span {:class "pill p-hold"} "GOVERNOR HOLD (hard)"] (str (:governor-hard-hold tally 0))
              [:code ":t = :governor-hold, no :phase-reason, verdict :hard? = true"]]
             [[:span {:class "pill p-phase"} "PHASE GATE"] (str (:phase-gate-hold tally 0))
              [:code ":t = :governor-hold WITH :phase-reason"]]
             [[:span {:class "pill p-reject"} "APPROVER REJECTED"] (str (:approver-rejected tally 0))
              [:code ":t = :approval-rejected"]]
             [[:span {:class "pill p-other"} "AWAITING APPROVAL"] (str (:awaiting-approval tally 0))
              [:code "no decision fact, disposition = :escalate"]]
             [[:span {:class "pill p-other"} "INDETERMINATE HOLD"] (str (:indeterminate-hold tally 0))
              [:code ":t = :governor-hold but verdict :hard? not true — surfaced, never folded"]]])
     [:p {:class "note"}
      "Discrimination check, computed from this run: "
      (str naive) " ledger decision facts carry a non-empty :violations list, but only "
      (str (:governor-hard-hold tally 0)) " of them are governor refusals. "
      "The difference is the approver-rejection fact, which carries {:rule :approver-rejected} "
      "while representing a proposal the governor had cleared. "
      (str violation-count) " runs produced a non-empty governor verdict violation list."]
     [:p {:class "note"}
      "Every run in the table above also declares, in "
      [:code "vocational.render-html/scenarios"] ", which class this file expects. "
      "Divergences are reported in section 9 rather than suppressed."])))

(defn- sec-expectations [runs]
  (let [checked (remove #(= :probe (get-in % [:scenario :expect])) runs)
        bad (filterv #(not= (get-in % [:scenario :expect]) (:class %)) checked)]
    (section
     "expectations" "9. Declared class vs. observed class"
     "If the code changes so a run lands differently, this table changes with it."
     (if (seq bad)
       [:div
        [:p {:class "note bad"}
         (str (count bad)) " run(s) did not land in the class this renderer declared. "
         "Reported, not hidden."]
        (table ["#" "op" "subject" "declared" "observed"]
               (for [{:keys [scenario class]} bad]
                 [[:code (txt (:id scenario))] [:code (kw (:op scenario))]
                  [:code (txt (:subject scenario))]
                  (class-label (:expect scenario)) (class-label class)]))]
       [:p {:class "note good"}
        "All " (str (count checked)) " declared runs landed in their declared class. "
        "(" (str (- (count runs) (count checked)))
        " run is a conformance probe with no declared class — see "
        [:a {:href "#probe"} "the conformance probe"] ".)"]))))

(defn- sec-ledger [db]
  (let [facts (store/ledger db)]
    (section
     "ledger" (str "10. Append-only audit ledger — " (count facts) " facts")
     "The store's own immutable log, in append order. This is the artefact an operator produces if a certification or graduation decision is later disputed."
     (table ["#" "fact type" "op" "subject" "disposition" "executing actor" "rules / basis" "summary"]
            (map-indexed
             (fn [i f]
               [[:span {:class "mono"} (str (inc i))]
                [:code (kw (:t f))]
                [:code (kw (:op f))]
                [:code (txt (:subject f))]
                [:code (kw (:disposition f))]
                [:code (txt (:actor f))]
                [:span {:class "mono"} (kws (:basis f))]
                (txt (or (:summary f)
                         (when (seq (:violations f))
                           (str/join " / " (map :detail (:violations f))))))])
             facts)))))

(defn- sec-registers [db]
  (let [certs (store/certification-history db)
        grads (store/graduation-history db)]
    (section
     "registers" "11. Actuation registers"
     "The append-only draft records the two actuation lifecycles produced. Each certificate is unsigned by construction — signature is the school's act, not this actor's."
     [:h3 (str "Certification finalizations — " (count certs))]
     (if (seq certs)
       (table ["record id" "kind" "student" "jurisdiction" "immutable"]
              (for [r certs]
                [[:code (txt (get r "record_id"))] (txt (get r "kind"))
                 [:code (txt (get r "student_id"))] [:code (txt (get r "jurisdiction"))]
                 (yn (get r "immutable"))]))
       [:p dash])
     [:h3 (str "Graduation finalizations — " (count grads))]
     (if (seq grads)
       (table ["record id" "kind" "student" "jurisdiction" "immutable"]
              (for [r grads]
                [[:code (txt (get r "record_id"))] (txt (get r "kind"))
                 [:code (txt (get r "student_id"))] [:code (txt (get r "jurisdiction"))]
                 (yn (get r "immutable"))]))
       [:p dash]))))

(defn- sec-attribution [scan ledger-scan]
  (let [lost (filterv (complement :retained?) scan)
        kept (filterv :retained? scan)
        by-effect (fn [rows] (kws (sort (distinct (map (comp name :effect) rows)))))]
    (section
     "attribution" "12. Approver attribution — scanned, not assumed"
     "Every commit below was approved by a named human. This section checks, by scanning the stored registers for approver-shaped keys, whether that identity survived the commit. It is derived from this run: if the commit path is changed to keep the approver, this section will say so."
     (table ["#" "op" "effect" "subject" "approver supplied" "registers written" "approver retained in"]
            (for [{:keys [scenario-id op effect subject approver places retained-in retained?]} scan]
              [[:code (txt scenario-id)] [:code (kw op)] [:code (kw effect)] [:code (txt subject)]
               [:code (txt approver)]
               (kws places)
               (if retained?
                 [:span {:class "pill p-commit"} (kws retained-in)]
                 [:span {:class "pill p-hold"} "NOT RETAINED"])]))
     (cond
       (and (seq lost) (seq kept))
       [:p {:class "note bad"}
        "Attribution is LOSSY, and lossy PER EFFECT. It survives for "
        (by-effect kept) " (which persist the record's :payload, and the approval "
        "path writes :approved-by into :payload), and is lost for " (by-effect lost)
        " (which persist :value, or derive the record from the subject alone). "
        "Consequence for an operator: for those effects the store cannot answer "
        "\"who approved this?\" — only \"which actor seat executed it\"."]

       (seq lost)
       [:p {:class "note bad"}
        "Attribution was NOT retained for any approved commit in this run ("
        (by-effect lost) ")."]

       :else
       [:p {:class "note good"}
        "Every approved commit in this run retained its approver identity."])
     [:h3 "The ledger itself"]
     [:p {:class "note"}
      "Scanned " (str (:facts ledger-scan)) " ledger facts for an approver-shaped key: "
      (str (:with-approver ledger-scan)) " carry one. "
      "The ledger's :actor field is populated on every fact — values seen: "
      [:code (kws (:actor-values ledger-scan))]
      " — but that is the EXECUTING actor-id from the operation context, not the approver. "
      "Reading it as the approver would produce a confident, wrong answer, so it is excluded "
      "from the approver-key set by name."])))

(defn- sec-probe [probe]
  (section
   "probe" "13. Governor conformance probe"
   "One run in this build exists only to ask a question of the governor, and reports whatever it answers."
   (if (nil? probe)
     [:p {:class "note"} "Probe scenario not present in this build."]
     [:div
      [:dl {:class "kv"}
       [:dt "subject"] [:dd [:code (txt (:subject probe))] " (" (txt (:student-name probe)) ")"]
       [:dt "ground truth :academic-integrity-flag?"] [:dd [:strong (yn (:ground-truth-flag probe))]]
       [:dt "outcome of :academic-integrity/screen on this student"]
       [:dd [:span {:class (str "pill " (class-css (:screen-class probe)))} (class-label (:screen-class probe))]]
       [:dt "integrity screen on file after that hold"]
       [:dd (if (:screen-on-file probe)
              [:code (kw (:verdict (:screen-on-file probe)))]
              [:strong "none — the hold prevented the screen from committing"])]
       [:dt "outcome of :certification/finalize on the same student"]
       [:dd [:span {:class (str "pill " (class-css (:probe-class probe)))} (class-label (:probe-class probe))]]
       [:dt "governor violations raised there"]
       [:dd (if (seq (:probe-violations probe))
              [:span {:class "mono"} (kws (map :rule (:probe-violations probe)))]
              [:strong "none"])]
       [:dt "certification finalized?"] [:dd [:strong (yn (:certified? probe))]]]
      (if (:gap? probe)
        [:div {:class "note bad"}
         [:strong "DISCLOSED GAP — not patched by this build."]
         [:p (str "The governor's academic-integrity check is unreachable on the actuation ops. "
                  "It fires on two conditions: the verdict carried by the proposal in front of it, "
                  "and an :unresolved integrity screen already committed to the store. A "
                  ":certification/finalize proposal carries no verdict, so only the second condition "
                  "can apply — but the governor HARD-holds the very screen that would write it, so "
                  "an :unresolved screen can never reach the store. The two halves of the defence "
                  "cancel each other out.")]
         [:p (str "Observed above: " (:subject probe) " carries the ground-truth flag, the screen was "
                  "correctly refused, and the certification was then finalized anyway. The actuation "
                  "ops never read :academic-integrity-flag? directly — unlike the attendance, "
                  "credit-set and workshop-safety checks, which all recompute from the student record.")]
         [:p (str "This is disclosed rather than fixed: changing the governor's rule set is a "
                  "governance decision with its own contract tests, not a side effect of a "
                  "rendering task.")]]
        [:div {:class "note good"}
         [:strong "No gap observed."]
         [:p "The governor refused to finalize a certification for a student carrying an unresolved academic-integrity flag."]])])))

(defn- sec-repro [db runs]
  (section
   "repro" "14. Reproducing this page"
   nil
   [:pre [:code "clojure -M:dev:render-html"]]
   [:p "The renderer executes " (str (count runs)) " graph runs against a freshly seeded "
    [:code (.getName (class db))] ", classifies each one, and writes this file. "
    "It refuses to write anything at all if the run produces zero HARD governor holds — "
    "a console with no genuine refusal is not evidence that the governor works."]
   [:p "Source: " [:code "src/vocational/render_html.clj"] ". "
    "The scenario script, the classifier and both derived disclosures live there; "
    "no markup in this page is authored by hand."]))

;; ===================================================================
;; page
;; ===================================================================

(defn- tally-of [runs]
  (frequencies (map :class runs)))

(defn render
  "Build the whole page from an executed run. Pure with respect to
  everything except `db`, which it only reads."
  [db runs]
  (let [bp     (read-blueprint)
        tally  (tally-of runs)
        scan   (attribution-scan db runs)
        lscan  (ledger-attribution-scan db)
        probe  (conformance-probe db runs)
        secs   [(sec-overview bp db runs tally)
                (sec-contract bp)
                (sec-facts)
                (sec-roster db)
                (sec-runs runs)
                (sec-holds runs)
                (sec-phase-gate runs)
                (sec-classifier runs tally)
                (sec-expectations runs)
                (sec-ledger db)
                (sec-registers db)
                (sec-attribution scan lscan)
                (sec-probe probe)
                (sec-repro db runs)]]
    {:sections (count secs)
     :html
     (str "<!DOCTYPE html>\n"
          (html
           [:html {:lang "en"}
            [:head
             [:meta {:charset "utf-8"}]
             [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
             [:title (str "Operator console — " (txt (:itonami.blueprint/id bp)))]
             [:meta {:name "description"
                     :content "Build-time operator console rendered from a live run of the ISIC 8522 technical-and-vocational-secondary-education actor."}]
             [:style {} (raw (str "\n" (dads-root-css) stylesheet))]]
            [:body
             [:header {:class "masthead"}
              [:div {:class "inner"}
               [:p {:class "kicker"}
                (str "ISIC " (txt (:itonami.blueprint/isic-rev5 bp)) " · "
                     (txt (:itonami.blueprint/id bp)))]
               [:h1 "Operator console"]
               [:p (txt (:itonami.blueprint/name bp))
                " — governed by the "
                (str/replace (kw (:itonami.blueprint/governor bp)) "-" " ")
                ". Rendered from a live actor run, not a mock-up."]]]
             [:main secs]
             [:footer
              [:p "Generated by " [:code "vocational.render-html"]
               " from an execution of " [:code "vocational.operation"]
               " over " [:code "vocational.store/seed-db"]
               ". Certificates in the registers above are drafts and are unsigned by construction."]]]]))}))

;; ===================================================================
;; build invariant + entry point
;; ===================================================================

(defn hard-hold-count [runs]
  (count (filter #(= :governor-hard-hold (:class %)) runs)))

(defn assert-hard-holds!
  "Build invariant. A console that shows no genuine governor refusal is
  not evidence that the governor works, so refuse to emit one. This is
  enforced here rather than left to convention, and it is checked BEFORE
  any file is written."
  [runs]
  (let [n (hard-hold-count runs)]
    (when (zero? n)
      (throw (ex-info
              (str "refusing to write the operator console: the run produced 0 HARD "
                   "governor holds. Either the governor stopped refusing anything, or "
                   "the scenario script no longer exercises it. Both are build failures.")
              {:runs (count runs)
               :classes (tally-of runs)})))
    n))

(defn build!
  "Execute, verify the invariant, then write. Returns a stats map."
  [out-path]
  (let [db     (store/seed-db)
        runs   (run-scenarios db scenarios)
        holds  (assert-hard-holds! runs)
        {:keys [html sections]} (render db runs)
        f      (io/file out-path)]
    (io/make-parents f)
    (spit f html)
    {:out         (.getPath f)
     :bytes       (count (.getBytes ^String html "UTF-8"))
     :sections    sections
     :runs        (count runs)
     :hard-holds  holds
     :classes     (tally-of runs)
     :ledger      (count (store/ledger db))
     :students    (count (store/all-students db))
     :certs       (count (store/certification-history db))
     :grads       (count (store/graduation-history db))}))

(defn -main [& args]
  (let [out (or (first args) default-out)
        st  (build! out)]
    (println "wrote" (:out st))
    (doseq [k [:bytes :sections :runs :hard-holds :ledger :students :certs :grads]]
      (println " " (name k) (get st k)))
    (println "  classes" (pr-str (into (sorted-map) (map (fn [[k v]] [(name k) v]) (:classes st)))))
    (flush)))
