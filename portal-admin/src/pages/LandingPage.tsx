import { useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Moon, Sun } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { usePrefs } from "@/lib/prefs";
import "@/styles/landing.css";

/** Picks the right copy variant from the inline {en, ar} pair. */
function useT() {
  const { i18n } = useTranslation();
  const lang = i18n.language?.startsWith("ar") ? "ar" : "en";
  return (en: string, ar: string) => (lang === "ar" ? ar : en);
}

export function LandingPage() {
  const T = useT();
  const { token } = useAuth();
  const { i18n } = useTranslation();
  const theme = usePrefs((s) => s.theme);
  const toggleTheme = usePrefs((s) => s.toggleTheme);
  const isDark = theme === "dark";
  const rootRef = useRef<HTMLDivElement>(null);

  // Wire the IntersectionObserver-driven .reveal animation that the
  // original static page used, scoped to this component's subtree.
  useEffect(() => {
    if (!rootRef.current) return;
    const els = rootRef.current.querySelectorAll<HTMLElement>(".reveal");
    const io = new IntersectionObserver(
      (entries) =>
        entries.forEach((e) => {
          if (e.isIntersecting) e.target.classList.add("visible");
        }),
      { threshold: 0.12 },
    );
    els.forEach((el) => io.observe(el));
    return () => io.disconnect();
  }, []);

  // Page-level dir/lang follow the portal's selected language so the
  // .motabiq-landing[dir=rtl] rules from the original CSS still apply.
  const dir = i18n.language?.startsWith("ar") ? "rtl" : "ltr";

  return (
    <div ref={rootRef} className="motabiq-landing" dir={dir}>
      <div className="noise" />

      <header className="site-header">
        <a className="brand" href="#home" aria-label="MOTABIQ Home">
          <span className="brand-mark">M</span>
          <span>
            <strong>MOTABIQ</strong>
            <em>مطابق</em>
          </span>
        </a>
        <nav className="nav">
          <a href="#platform">{T("Platform", "المنصة")}</a>
          <a href="#usecases">{T("Use Cases", "حالات الاستخدام")}</a>
          <a href="#security">{T("Security", "الأمن")}</a>
          <a href="#architecture">{T("Architecture", "المعمارية")}</a>
          <a href="#integration">{T("Integration", "التكامل")}</a>
          <Link to={token ? "/dashboard" : "/login"} className="nav-cta">
            {token
              ? T("Open Portal", "فتح اللوحة")
              : T("Sign in", "تسجيل الدخول")}
          </Link>
        </nav>
        <div className="header-tools">
          <button
            type="button"
            className="theme-btn"
            aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
            onClick={toggleTheme}
          >
            {isDark ? (
              <>
                <Sun size={14} />
                <span>{T("Light", "فاتح")}</span>
              </>
            ) : (
              <>
                <Moon size={14} />
                <span>{T("Dark", "داكن")}</span>
              </>
            )}
          </button>
          <button
            className="lang-toggle"
            aria-label="Toggle language"
            onClick={() => i18n.changeLanguage(dir === "rtl" ? "en" : "ar")}
          >
            {dir === "rtl" ? "English" : "العربية"}
          </button>
        </div>
      </header>

      <main id="home">
        <section className="hero section-pad">
          <div className="hero-copy reveal">
            <span className="eyebrow">
              {T(
                "National Digital Trust Infrastructure",
                "بنية تحتية وطنية للثقة الرقمية",
              )}
            </span>
            <h1>
              {T(
                "Biometric Verification with National Trust",
                "التحقق الحيوي بثقة وطنية",
              )}
            </h1>
            <p>
              {T(
                "MOTABIQ is a sovereign-grade multi-modal biometric verification platform enabling government entities, banks, fintechs and regulated institutions to verify identities securely, instantly and without exposing original biometric records.",
                "مطابق هي منصة وطنية للتحقق الحيوي متعدد السمات، تمكّن الجهات الحكومية والبنوك وشركات التقنية المالية والجهات المنظمة من التحقق من الهوية بأمان وسرعة ودون كشف السجلات الحيوية الأصلية.",
              )}
            </p>
            <div className="hero-actions">
              <a href="#contact" className="btn primary">
                {T("Request a Demo", "طلب عرض توضيحي")}
              </a>
              <a href="#architecture" className="btn ghost">
                {T("Explore Architecture", "استعراض المعمارية")}
              </a>
            </div>
            <div className="trust-strip">
              <div>
                <b>99.9%</b>
                <span>{T("Target Availability", "إتاحة مستهدفة")}</span>
              </div>
              <div>
                <b>API</b>
                <span>{T("Secure Integration", "تكامل آمن")}</span>
              </div>
              <div>
                <b>Match</b>
                <span>{T("No Raw Data Sharing", "دون مشاركة بيانات خام")}</span>
              </div>
            </div>
          </div>
          <div className="hero-visual reveal delay-1">
            <img src="/motabiq-3d-logo.png" alt="MOTABIQ 3D Logo" />
            <div className="orb orb-1" />
            <div className="orb orb-2" />
          </div>
        </section>

        <section className="logos section-pad thin">
          <span>{T("Designed for", "مصممة من أجل")}</span>
          <b>{T("Government", "الحكومة")}</b>
          <b>{T("Banking", "البنوك")}</b>
          <b>{T("Fintech", "التقنية المالية")}</b>
          <b>{T("Telecom", "الاتصالات")}</b>
          <b>{T("National Platforms", "المنصات الوطنية")}</b>
        </section>

        <section id="platform" className="section-pad split">
          <div className="reveal">
            <span className="eyebrow">{T("Platform Essence", "جوهر المنصة")}</span>
            <h2>
              {T(
                "Not a fingerprint system. A national verification layer.",
                "ليست نظام بصمة. بل طبقة تحقق وطنية.",
              )}
            </h2>
            <p>
              {T(
                "MOTABIQ supports multiple biometric modalities — face, iris, fingerprint, voice and future traits — through one governed platform. Beneficiary institutions receive only a trusted verification response, while sensitive biometric records remain protected at the authorized national source.",
                "تدعم مطابق عدة سمات حيوية — الوجه، القزحية، البصمة، الصوت، وأنواع مستقبلية — ضمن منصة واحدة محوكمة. تستلم الجهات المستفيدة نتيجة تحقق موثوقة فقط، بينما تبقى السجلات الحيوية الحساسة محمية لدى المصدر الوطني المعتمد.",
              )}
            </p>
          </div>
          <div className="modality-grid reveal delay-1">
            <div className="modality">
              <span>◉</span>
              <b>{T("Face", "الوجه")}</b>
            </div>
            <div className="modality">
              <span>◎</span>
              <b>{T("Iris", "القزحية")}</b>
            </div>
            <div className="modality">
              <span>≋</span>
              <b>{T("Voice", "الصوت")}</b>
            </div>
            <div className="modality">
              <span>✦</span>
              <b>{T("Future Traits", "سمات مستقبلية")}</b>
            </div>
          </div>
        </section>

        <section className="section-pad cards-section">
          <div className="section-head reveal">
            <span className="eyebrow">
              {T("Core Capabilities", "القدرات الأساسية")}
            </span>
            <h2>
              {T(
                "Built for regulated, high-volume identity verification",
                "مصممة للتحقق المنظم واسع النطاق",
              )}
            </h2>
          </div>
          <div className="cards">
            {[
              ["Multi-Modal Matching", "مطابقة متعددة السمات",
               "Verify using face, iris, fingerprint, voice or combined verification policies.",
               "تحقق باستخدام الوجه أو القزحية أو البصمة أو الصوت أو سياسات تحقق مركبة."],
              ["Match / No-Match", "مطابق / غير مطابق",
               "Return a trusted result without exposing original biometric templates or raw records.",
               "إرجاع نتيجة موثوقة دون كشف القوالب الحيوية أو السجلات الخام."],
              ["Policy Engine", "محرك السياسات",
               "Control who can verify, when, why, and under which legal or operational basis.",
               "التحكم بمن يحق له التحقق، ومتى، ولماذا، ووفق أي أساس نظامي أو تشغيلي."],
              ["Audit & Governance", "التدقيق والحوكمة",
               "Immutable-style logs, transaction evidence and compliance dashboards.",
               "سجلات تدقيق، أدلة عمليات، ولوحات امتثال للجهات الرقابية."],
              ["Secure API Gateway", "بوابة API آمنة",
               "Certificates, throttling, authentication, authorization and integration monitoring.",
               "شهادات، تحديد معدلات، مصادقة، تفويض، ومراقبة تكامل."],
              ["Operational Console", "لوحة تشغيلية",
               "Institution onboarding, sandbox access, SLA tracking and service analytics.",
               "تسجيل الجهات، بيئة اختبار، متابعة SLA، وتحليلات الخدمة."],
            ].map(([titleEn, titleAr, bodyEn, bodyAr], i) => (
              <article key={titleEn} className="card reveal">
                <i>{String(i + 1).padStart(2, "0")}</i>
                <h3>{T(titleEn, titleAr)}</h3>
                <p>{T(bodyEn, bodyAr)}</p>
              </article>
            ))}
          </div>
        </section>

        <section id="usecases" className="section-pad dark-panel">
          <div className="section-head reveal">
            <span className="eyebrow">{T("Use Cases", "حالات الاستخدام")}</span>
            <h2>
              {T(
                "One trusted verification layer across critical sectors",
                "طبقة تحقق موثوقة عبر القطاعات الحرجة",
              )}
            </h2>
          </div>
          <div className="use-grid">
            {[
              ["Banking & eKYC", "البنوك و eKYC",
               "Account opening, customer updates, high-risk transactions and fraud reduction.",
               "فتح الحسابات، تحديث العملاء، العمليات عالية المخاطر، وتقليل الاحتيال."],
              ["Government Services", "الخدمات الحكومية",
               "Benefits eligibility, registry updates, duplicate prevention and digital service assurance.",
               "استحقاق الخدمات، تحديث السجلات، منع التكرار، وضمان الخدمات الرقمية."],
              ["Telecom Activation", "تفعيل خدمات الاتصالات",
               "SIM issuance, subscriber verification and fake registration prevention.",
               "إصدار الشرائح، التحقق من المشتركين، ومنع التسجيل الوهمي."],
              ["Border & Security", "المنافذ والأمن",
               "Identity assurance for controlled environments and sensitive security procedures.",
               "ضمان الهوية في البيئات المنظمة والإجراءات الأمنية الحساسة."],
            ].map(([titleEn, titleAr, bodyEn, bodyAr]) => (
              <div key={titleEn} className="use-card reveal">
                <h3>{T(titleEn, titleAr)}</h3>
                <p>{T(bodyEn, bodyAr)}</p>
              </div>
            ))}
          </div>
        </section>

        <section id="security" className="section-pad split security-section">
          <div className="dashboard reveal">
            <div className="dash-top">
              <span /><span /><span />
            </div>
            <h3>
              {T("Verification Operations Console", "لوحة عمليات التحقق")}
            </h3>
            <div className="meter">
              <b>98.7%</b>
              <small>{T("Successful Verifications", "عمليات تحقق ناجحة")}</small>
            </div>
            <div className="mini-bars">
              <span /><span /><span /><span /><span />
            </div>
            <div className="dash-row">
              <b>{T("Risk Policy", "سياسة المخاطر")}</b>
              <em>{T("Active", "مفعلة")}</em>
            </div>
            <div className="dash-row">
              <b>{T("Audit Stream", "تدفق التدقيق")}</b>
              <em>{T("Live", "مباشر")}</em>
            </div>
          </div>
          <div className="reveal delay-1">
            <span className="eyebrow">
              {T("Security & Privacy", "الأمن والخصوصية")}
            </span>
            <h2>
              {T("Privacy-preserving by design", "الخصوصية في صميم التصميم")}
            </h2>
            <p>
              {T(
                "MOTABIQ follows minimal disclosure principles. The consuming institution does not receive raw biometric data; it receives a governed verification decision with full auditability.",
                "تعتمد مطابق مبدأ الحد الأدنى من الإفصاح. لا تستلم الجهة المستفيدة البيانات الحيوية الخام، بل تستلم قرار تحقق محوكمًا مع قابلية تدقيق كاملة.",
              )}
            </p>
            <ul className="check-list">
              <li>{T("Encryption in transit and at rest", "تشفير أثناء النقل والتخزين")}</li>
              <li>{T("Role-based access control", "صلاحيات مبنية على الأدوار")}</li>
              <li>{T("Certificate-based API authentication", "مصادقة API قائمة على الشهادات")}</li>
              <li>{T("Complete transaction audit trail", "سجل تدقيق كامل لكل عملية")}</li>
              <li>{T("Segregated Dev, Test and Production environments", "بيئات منفصلة للتطوير والاختبار والإنتاج")}</li>
            </ul>
          </div>
        </section>

        <section id="architecture" className="section-pad architecture">
          <div className="section-head reveal">
            <span className="eyebrow">
              {T("Reference Architecture", "المعمارية المرجعية")}
            </span>
            <h2>
              {T("A governed national verification flow", "تدفق تحقق وطني محوكم")}
            </h2>
          </div>
          <div className="arch-diagram reveal">
            <div className="arch-node">
              <b>{T("Banks / Gov / Fintech", "بنوك / حكومة / تقنية مالية")}</b>
              <span>{T("Consuming Entities", "الجهات المستفيدة")}</span>
            </div>
            <div className="arch-arrow">→</div>
            <div className="arch-node hot">
              <b>{T("Secure API Gateway", "بوابة API آمنة")}</b>
              <span>{T("Auth • Rate Limit • Logs", "مصادقة • حدود • سجلات")}</span>
            </div>
            <div className="arch-arrow">→</div>
            <div className="arch-node">
              <b>{T("Verification Engine", "محرك التحقق")}</b>
              <span>{T("Matching • Policy • Risk", "مطابقة • سياسة • مخاطر")}</span>
            </div>
            <div className="arch-arrow">→</div>
            <div className="arch-node">
              <b>{T("National Data Source", "مصدر البيانات الوطني")}</b>
              <span>{T("Protected Biometric Records", "سجلات حيوية محمية")}</span>
            </div>
          </div>
          <div className="governance-band reveal">
            <span>{T("Audit & Governance Layer", "طبقة التدقيق والحوكمة")}</span>
            <span>{T("SLA Monitoring", "مراقبة SLA")}</span>
            <span>{T("Regulatory Reports", "تقارير تنظيمية")}</span>
            <span>{T("Institutional Continuity", "استمرارية مؤسسية")}</span>
          </div>
        </section>

        <section id="integration" className="section-pad timeline-section">
          <div className="section-head reveal">
            <span className="eyebrow">
              {T("Integration Journey", "رحلة التكامل")}
            </span>
            <h2>
              {T(
                "From approval to production go-live",
                "من الاعتماد إلى التشغيل الفعلي",
              )}
            </h2>
          </div>
          <div className="timeline reveal">
            {[
              ["Registration & Approval", "التسجيل والاعتماد"],
              ["Sandbox Access", "الوصول لبيئة الاختبار"],
              ["API Certificates", "شهادات API"],
              ["Testing & Certification", "الاختبار والتصديق"],
              ["Production Go-Live", "الإطلاق الإنتاجي"],
              ["SLA & Monitoring", "SLA والمراقبة"],
            ].map(([en, ar], i) => (
              <div key={en}>
                <b>{i + 1}</b>
                <span>{T(en, ar)}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="section-pad products">
          <div className="section-head reveal">
            <span className="eyebrow">{T("MOTABIQ Suite", "حزمة مطابق")}</span>
            <h2>
              {T(
                "A complete national verification ecosystem",
                "منظومة تحقق وطنية متكاملة",
              )}
            </h2>
          </div>
          <div className="product-grid">
            <div className="product-card reveal">
              <h3>MOTABIQ Verify</h3>
              <p>{T("Real-time biometric verification APIs.", "واجهات تحقق حيوي لحظية.")}</p>
            </div>
            <div className="product-card reveal">
              <h3>MOTABIQ Onboarding</h3>
              <p>{T("Digital onboarding and enrollment support.", "دعم التسجيل والالتحاق الرقمي.")}</p>
            </div>
            <div className="product-card reveal">
              <h3>MOTABIQ Risk</h3>
              <p>{T("Risk-based verification and escalation rules.", "تحقق مبني على المخاطر وقواعد التصعيد.")}</p>
            </div>
            <div className="product-card reveal">
              <h3>MOTABIQ Audit</h3>
              <p>{T("Regulatory reporting and transaction evidence.", "تقارير تنظيمية وأدلة عمليات.")}</p>
            </div>
          </div>
        </section>

        <section id="contact" className="section-pad contact-section">
          <div className="contact-card reveal">
            <span className="eyebrow">{T("Start the Conversation", "ابدأ النقاش")}</span>
            <h2>
              {T(
                "Bring national-grade biometric verification to your institution",
                "اجلب التحقق الحيوي الوطني إلى جهتك",
              )}
            </h2>
            <p>
              {T(
                "Request a strategic demo, technical integration session, or executive briefing for your leadership team.",
                "اطلب عرضًا استراتيجيًا أو جلسة تكامل تقنية أو إحاطة تنفيذية لفريق القيادة.",
              )}
            </p>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                alert(
                  T(
                    "Demo request captured. Our team will reach out shortly.",
                    "تم استلام الطلب. سيتواصل فريقنا معكم قريبًا.",
                  ),
                );
              }}
            >
              <input placeholder={T("Full name", "الاسم الكامل")} />
              <input placeholder={T("Work email", "البريد الرسمي")} />
              <select>
                <option>{T("Government", "جهة حكومية")}</option>
                <option>{T("Bank", "بنك")}</option>
                <option>{T("Fintech", "تقنية مالية")}</option>
                <option>{T("Telecom", "اتصالات")}</option>
              </select>
              <button className="btn primary" type="submit">
                {T("Submit Request", "إرسال الطلب")}
              </button>
            </form>
          </div>
        </section>
      </main>

      <footer className="footer">
        <div>
          <b>MOTABIQ | مطابق</b>
          <span>
            {T(
              "Biometric Verification with National Trust",
              "التحقق الحيوي بثقة وطنية",
            )}
          </span>
        </div>
        <p>© {new Date().getFullYear()} MOTABIQ. {T("All rights reserved.", "جميع الحقوق محفوظة.")}</p>
      </footer>
    </div>
  );
}
