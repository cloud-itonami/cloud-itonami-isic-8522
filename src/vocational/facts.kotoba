(ns vocational.facts
  "Per-jurisdiction technical-and-vocational-secondary-education
  licensing catalog -- the G2-style spec-basis table the Curriculum
  Safeguarding Governor checks every `:jurisdiction/assess` proposal
  against ('did the advisor cite an OFFICIAL public source for this
  jurisdiction's vocational-education certification/graduation
  requirements, or did it invent one?'), closely modeled on
  `cloud-itonami-isic-8521`'s `secondary.facts`.

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Beyond `secondary.facts`'s own general-secondary-education catalog,
  this catalog ALSO cites each jurisdiction's official workplace/
  workshop-safety-training regulator -- the genuinely new concern this
  vertical adds. Technical and vocational secondary education
  characteristically involves hands-on practicum work with tools,
  machinery and industrial/workshop equipment (the defining nature of
  ISIC 8522 itself), a concern essentially ABSENT from `secondary`/
  8521's own general-academic-curriculum focus. Unlike some other
  recent additions to this fleet's unconditional-evaluation-discipline
  family, this check is deliberately NOT conditional on a per-student
  ground truth: every student in a technical/vocational program is, by
  the vertical's own definition, engaged in hands-on shop/practicum
  work, so workplace-safety-training-certification applies
  unconditionally to every student here -- unlike, say,
  `socialresearch`'s human-subjects-review (not every study involves
  human subjects) or `bizassoc`'s lobbying-registration (not every
  jurisdiction has a registration regime).")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  student-enrollment/curriculum-approval/attendance-record/academic-
  transcript evidence set (PLUS a workshop-safety-training-record
  item); `:legal-basis` / `:owner-authority` / `:provenance` are the
  G2 citation the governor requires before any `:jurisdiction/assess`
  proposal can commit. `:safety-owner-authority` /
  `:safety-legal-basis` / `:safety-provenance` are the SEPARATE
  workplace/workshop-safety-training citation the governor's
  `workplace-safety-training-unconfirmed?` check is grounded in."
  {"JPN" {:name "Japan"
          :owner-authority "文部科学省 (Ministry of Education, Culture, Sports, Science and Technology, MEXT)"
          :legal-basis "職業能力開発促進法 (Act on Human Resources Development Promotion)"
          :national-spec "専門高校・職業訓練校の実習・実技指導基準"
          :provenance "https://www.mext.go.jp/"
          :required-evidence ["生徒在籍記録 (student-enrollment record)"
                              "教育課程編成届 (curriculum-approval certificate)"
                              "出席記録 (attendance record)"
                              "実習安全訓練記録 (workshop-safety-training record)"
                              "成績・単位修得証明書 (academic-transcript document)"]
          :safety-owner-authority "厚生労働省 (Ministry of Health, Labour and Welfare)"
          :safety-legal-basis "労働安全衛生法 (Industrial Safety and Health Act)"
          :safety-provenance "https://www.mhlw.go.jp/stf/seisakunitsuite/bunya/koyou_roudou/roudoukijun/anzeneisei/"}
   "USA" {:name "United States"
          :owner-authority "State Departments of Education (secondary career-and-technical-education authority)"
          :legal-basis "State career-and-technical-education (CTE) credentialing statutes under the Carl D. Perkins Career and Technical Education Act framework"
          :national-spec "State CTE credit-hour, attendance and certification-requirement standards"
          :provenance "https://www.ed.gov/laws-and-policy/higher-education/perkins-cte"
          :required-evidence ["Student-enrollment record"
                              "Curriculum-approval certificate"
                              "Attendance record"
                              "Workshop-safety-training record"
                              "Academic-transcript document"]
          :safety-owner-authority "Occupational Safety and Health Administration (OSHA), U.S. Department of Labor"
          :safety-legal-basis "29 C.F.R. Part 1910 (General Industry) / Part 1926 (Construction) OSHA standards"
          :safety-provenance "https://www.osha.gov/laws-regs/regulations/standardnumber"}
   "GBR" {:name "United Kingdom"
          :owner-authority "Office of Qualifications and Examinations Regulation (Ofqual)"
          :legal-basis "Apprenticeships, Skills, Children and Learning Act 2009 (Ofqual's regulatory framework, technical qualifications)"
          :national-spec "T Level/vocational-qualification regulation and grading standards"
          :provenance "https://www.gov.uk/government/organisations/ofqual"
          :required-evidence ["Student-enrollment record"
                              "Curriculum-approval certificate"
                              "Attendance record"
                              "Workshop-safety-training record"
                              "Academic-transcript document"]
          :safety-owner-authority "Health and Safety Executive (HSE)"
          :safety-legal-basis "Health and Safety at Work etc. Act 1974"
          :safety-provenance "https://www.hse.gov.uk/education/index.htm"}
   "DEU" {:name "Germany"
          :owner-authority "Kultusministerien der Länder (state ministries of education and cultural affairs)"
          :legal-basis "Berufsbildungsgesetz (BBiG) -- Ausbildungsordnungen der Länder (state vocational-training regulations)"
          :national-spec "Abschluss- und Notengebungsvorgaben für berufsbildende Schulen der Länder"
          :provenance "https://www.kmk.org/"
          :required-evidence ["Schülereinschreibung (student-enrollment record)"
                              "Lehrplangenehmigung (curriculum-approval certificate)"
                              "Anwesenheitsprotokoll (attendance record)"
                              "Werkstattsicherheitsschulungsprotokoll (workshop-safety-training record)"
                              "Zeugnis/Notennachweis (academic-transcript document)"]
          :safety-owner-authority "Deutsche Gesetzliche Unfallversicherung (DGUV) / Berufsgenossenschaften"
          :safety-legal-basis "Sozialgesetzbuch VII (SGB VII) -- DGUV Vorschriften für Ausbildungsstätten und Schulwerkstätten"
          :safety-provenance "https://www.dguv.de/"}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to finalize a
  certification or graduation decision on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-8522 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `vocational.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
