// Support error playbook — bilingual (en/ar) content for the admin Support page.
// Kept as a self-contained data module so the catalog stays maintainable and
// does not bloat the i18n locale files. The page picks the language at runtime.

export type Bi = { en: string; ar: string };
export type Severity = "guide" | "account" | "escalate";
export type EscalateLevel = "no" | "maybe" | "yes";

export type PlaybookEntry = {
  code: number;
  http: number;
  enumName: string;
  category: string;
  severity: Severity;
  escalateLevel: EscalateLevel;
  keywords: string; // for search — both languages
  name: Bi;
  meaning: Bi;
  action: Bi;
  reply: Bi; // copy-ready customer message
  escalate: Bi; // the escalation guidance line
};

export type Category = { key: string; title: Bi; hint: Bi };

export const CATEGORIES: Category[] = [
  { key: "request", title: { en: "Request & Biometrics", ar: "الطلب والبصمة" }, hint: { en: "the data or the fingerprint", ar: "البيانات أو البصمة" } },
  { key: "auth", title: { en: "Authentication & Access", ar: "المصادقة والوصول" }, hint: { en: "keys, IPs, and plan permissions", ar: "المفاتيح وعناوين IP وصلاحيات الباقة" } },
  { key: "pin", title: { en: "Client Portal — PIN", ar: "بوابة العميل — رمز PIN" }, hint: { en: "only on the portal Transactions page", ar: "فقط في صفحة المعاملات داخل البوابة" } },
  { key: "billing", title: { en: "Plan & Billing", ar: "الباقة والفوترة" }, hint: { en: "quotas, wallet, and conflicts", ar: "الحصص والمحفظة والتعارضات" } },
  { key: "system", title: { en: "System & Backend", ar: "النظام والخدمة الخلفية" }, hint: { en: "our platform or Yemen ID — escalate these", ar: "منصّتنا أو الهوية اليمنية — تُصعّد" } },
];

export const ENTRIES: PlaybookEntry[] = [
  // ── Request & Biometrics ──────────────────────────────────────────────────
  {
    code: 1002, http: 400, enumName: "VALIDATION_FAILED", category: "request", severity: "guide", escalateLevel: "maybe",
    keywords: "1002 invalid biometrics moi 220 bad image quality fingerprint recapture validation بصمة غير صالحة جودة",
    name: { en: "Validation failed / Invalid biometrics", ar: "فشل التحقّق / بصمة غير صالحة" },
    meaning: { en: "A field failed validation, or — most often — the fingerprint image couldn't be read. The message shows “MOI 220 · Invalid biometrics” or “203 · Bad image format” for the biometric case.", ar: "فشل التحقّق من أحد الحقول، أو — في الغالب — تعذّرت قراءة صورة البصمة. تظهر الرسالة «MOI 220 · Invalid biometrics» أو «203 · Bad image format» في حالة البصمة." },
    action: { en: "Ask the customer to recapture the fingerprint: clean the sensor, place the finger flat and firm, use the correct finger, and send a valid WSQ image. For a field error, point them to the field named in the message.", ar: "اطلب من العميل إعادة التقاط البصمة: تنظيف الماسح، ووضع الإصبع بشكل مسطّح وثابت، واستخدام الإصبع الصحيح، وإرسال صورة WSQ صالحة. أما خطأ الحقل فوجّهه إلى الحقل المذكور في الرسالة." },
    reply: { en: "The fingerprint couldn't be read clearly. Please clean the sensor, place the finger flat and firmly, and recapture the print, then try again. Make sure it's the correct finger for this person.", ar: "تعذّرت قراءة البصمة بوضوح. يرجى تنظيف الماسح ووضع الإصبع بشكل مسطّح وثابت، ثم إعادة التقاط البصمة والمحاولة مجددًا. تأكّد من أنها الإصبع الصحيح لهذا الشخص." },
    escalate: { en: "Only if good-quality captures keep failing — then send dev the requestId + a sample.", ar: "فقط إذا استمر الفشل مع صور عالية الجودة — عندها أرسل للمطوّر رقم الطلب (requestId) وعيّنة." },
  },
  {
    code: 1302, http: 422, enumName: "BIOMETRIC_NO_MATCH", category: "request", severity: "guide", escalateLevel: "no",
    keywords: "1302 no match biometric fingerprint does not match wrong finger 422 unprocessable لا تطابق بصمة",
    name: { en: "Fingerprint did not match the ID", ar: "البصمة لا تطابق الرقم الوطني" },
    meaning: { en: "The national ID exists, but the fingerprint did not match that person. This is a correct, deliberate rejection — not a fault. No personal data is returned.", ar: "الرقم الوطني موجود، لكن البصمة لم تطابق ذلك الشخص. هذا رفضٌ صحيح ومقصود وليس خطأً. ولا تُرجَع أي بيانات شخصية." },
    action: { en: "Confirm the right person and finger are being used, then recapture and retry. Repeated no-match usually means the wrong person/finger or a poor capture.", ar: "تأكّد من استخدام الشخص والإصبع الصحيحين، ثم أعد الالتقاط وحاول مجددًا. تكرار عدم التطابق يعني غالبًا شخصًا/إصبعًا خاطئًا أو التقاطًا رديئًا." },
    reply: { en: "The fingerprint did not match this national ID. Please confirm you're verifying the correct person and finger, recapture the print, and try again.", ar: "البصمة لم تطابق هذا الرقم الوطني. يرجى التأكّد من أنك تتحقّق من الشخص والإصبع الصحيحين، ثم إعادة التقاط البصمة والمحاولة مجددًا." },
    escalate: { en: "No — this is a genuine result from Yemen ID. Do not override a no-match.", ar: "لا — هذه نتيجة حقيقية من الهوية اليمنية. لا تتجاوز نتيجة عدم التطابق." },
  },
  {
    code: 1301, http: 404, enumName: "NOT_FOUND", category: "request", severity: "guide", escalateLevel: "no",
    keywords: "1301 not found national number id yemen id 404 wrong number الرقم الوطني غير موجود",
    name: { en: "National number not found", ar: "الرقم الوطني غير موجود" },
    meaning: { en: "The national ID number was not found in the Yemen ID database.", ar: "لم يُعثر على الرقم الوطني في قاعدة بيانات الهوية اليمنية." },
    action: { en: "Check the number was entered correctly — 12 digits, format NNNN-NNNN-NNNN. A typo is the usual cause.", ar: "تحقّق من إدخال الرقم بشكل صحيح — ١٢ خانة بالصيغة NNNN-NNNN-NNNN. غالبًا يكون السبب خطأً مطبعيًا." },
    reply: { en: "This national ID number was not found. Please double-check the number (12 digits) and try again.", ar: "لم يُعثر على هذا الرقم الوطني. يرجى التحقّق من الرقم (١٢ خانة) والمحاولة مجددًا." },
    escalate: { en: "No — a real result from Yemen ID.", ar: "لا — نتيجة حقيقية من الهوية اليمنية." },
  },
  {
    code: 1001, http: 400, enumName: "BAD_REQUEST", category: "request", severity: "guide", escalateLevel: "maybe",
    keywords: "1001 bad request malformed json missing field 400 format طلب غير صحيح",
    name: { en: "Bad request", ar: "طلب غير صحيح" },
    meaning: { en: "The request wasn't formatted correctly — malformed JSON, a missing required field, or a wrong data type.", ar: "لم يُنسّق الطلب بشكل صحيح — JSON غير صالح، أو حقل مطلوب مفقود، أو نوع بيانات خاطئ." },
    action: { en: "Have the client check their request body against the API docs / Postman collection: valid JSON, required fields present, correct field names.", ar: "اطلب من العميل مراجعة جسم الطلب وفق وثائق الـ API / مجموعة Postman: JSON صالح، ووجود الحقول المطلوبة، وأسماء حقول صحيحة." },
    reply: { en: "The request wasn't formatted correctly. Please check it against the integration guide — valid JSON and all required fields — then try again.", ar: "لم يُنسّق الطلب بشكل صحيح. يرجى مراجعته وفق دليل التكامل — JSON صالح وجميع الحقول المطلوبة — ثم المحاولة مجددًا." },
    escalate: { en: "Only if the request genuinely looks correct — send dev the requestId.", ar: "فقط إذا كان الطلب يبدو صحيحًا فعلًا — أرسل للمطوّر رقم الطلب (requestId)." },
  },

  // ── Authentication & Access ───────────────────────────────────────────────
  {
    code: 1101, http: 401, enumName: "UNAUTHENTICATED", category: "auth", severity: "guide", escalateLevel: "no",
    keywords: "1101 unauthenticated missing api key auth header 401 basic المصادقة مطلوبة مفتاح",
    name: { en: "Authentication required", ar: "المصادقة مطلوبة" },
    meaning: { en: "No credentials were sent, or the auth header was malformed. The client isn't sending their API key correctly.", ar: "لم تُرسَل بيانات اعتماد، أو كانت ترويسة المصادقة غير صحيحة. العميل لا يرسل مفتاح الـ API بشكل صحيح." },
    action: { en: "Confirm they use HTTP Basic auth — clientId as username, clientSecret as password — on every call.", ar: "تأكّد من استخدامهم مصادقة HTTP Basic — معرّف العميل (clientId) كاسم مستخدم، والسر (clientSecret) ككلمة مرور — في كل طلب." },
    reply: { en: "Your request is missing authentication. Please send your API key using HTTP Basic auth — client ID as the username and client secret as the password — on every request.", ar: "طلبك يفتقد المصادقة. يرجى إرسال مفتاح الـ API باستخدام HTTP Basic — معرّف العميل كاسم مستخدم والسر ككلمة مرور — في كل طلب." },
    escalate: { en: "No.", ar: "لا." },
  },
  {
    code: 1102, http: 401, enumName: "INVALID_CREDENTIALS", category: "auth", severity: "account", escalateLevel: "maybe",
    keywords: "1102 invalid credentials wrong secret revoked expired reissue key 401 token بيانات اعتماد غير صحيحة",
    name: { en: "Invalid credentials", ar: "بيانات اعتماد غير صحيحة" },
    meaning: { en: "The clientId/clientSecret is wrong, revoked, or expired. (Rarely, on verify, it means Yemen ID rejected our internal token — that's on us.)", ar: "معرّف/سر العميل خاطئ أو مُلغى أو منتهٍ. (نادرًا، عند التحقّق، يعني أن الهوية اليمنية رفضت رمزنا الداخلي — وهذا من جهتنا.)" },
    action: { en: "Confirm the client ID. If the secret was lost or the key revoked, arrange a re-issue. If the message references MOI / token, treat it as a backend issue and escalate.", ar: "تأكّد من معرّف العميل. إذا فُقد السر أو أُلغي المفتاح، رتّب إصدارًا جديدًا. وإذا أشارت الرسالة إلى MOI / token فاعتبرها مشكلة في الخدمة الخلفية وصعّدها." },
    reply: { en: "Your API credentials were not accepted. Please double-check your client ID and secret. If the secret was lost, we can issue you a new key — let us know.", ar: "لم تُقبل بيانات اعتماد الـ API. يرجى التحقّق من معرّف العميل والسر. وإن فُقد السر يمكننا إصدار مفتاح جديد لك — أخبِرنا." },
    escalate: { en: "Only if the credentials are confirmed correct — possible Yemen ID token issue → dev/ops.", ar: "فقط إذا تأكّدت صحّة البيانات — قد تكون مشكلة رمز الهوية اليمنية → المطوّر/التشغيل." },
  },
  {
    code: 1201, http: 403, enumName: "FORBIDDEN", category: "auth", severity: "account", escalateLevel: "maybe",
    keywords: "1201 forbidden ip allowlist allow list not allowed 403 whitelist egress الوصول مرفوض عنوان",
    name: { en: "Access denied (IP allow-list)", ar: "الوصول مرفوض (قائمة IP)" },
    meaning: { en: "The request was blocked — most commonly the caller's IP address isn't on the allow-list for their API key.", ar: "حُجب الطلب — غالبًا لأن عنوان IP للطالب ليس ضمن القائمة المسموح بها لمفتاح الـ API." },
    action: { en: "Ask for the client's server (egress) IP and add it to their credential's allow-list, or confirm the existing entry.", ar: "اطلب عنوان IP الصادر من خادم العميل وأضِفه إلى القائمة المسموح بها لمفتاحه، أو تأكّد من الإدخال الحالي." },
    reply: { en: "Access was denied — your server's IP address may not be on the approved list for your API key. Please share the IP address your requests come from and we'll allow it.", ar: "رُفض الوصول — قد لا يكون عنوان IP لخادمك ضمن القائمة المعتمدة لمفتاحك. يرجى مشاركة عنوان IP الذي تأتي منه طلباتك وسنقوم بالسماح به." },
    escalate: { en: "To ops — to update the allow-list (not a bug).", ar: "إلى فريق التشغيل — لتحديث القائمة (ليست خطأً برمجيًا)." },
  },
  {
    code: 1202, http: 403, enumName: "ENTITLEMENT_DENIED", category: "auth", severity: "account", escalateLevel: "no",
    keywords: "1202 entitlement plan subscription not entitled upgrade 403 operation غير مشمول الباقة الاشتراك",
    name: { en: "Not entitled by plan", ar: "غير مشمول بالباقة" },
    meaning: { en: "The client's subscription/plan doesn't include the operation they called (or the subscription is inactive/expired).", ar: "اشتراك/باقة العميل لا تشمل العملية المطلوبة (أو أن الاشتراك غير نشط/منتهٍ)." },
    action: { en: "Check their active subscription and plan in the admin portal. Route upgrades to sales/ops.", ar: "تحقّق من اشتراكه وباقته النشطة في بوابة الإدارة. ووجّه طلبات الترقية إلى المبيعات/التشغيل." },
    reply: { en: "Your current plan doesn't include this operation, or your subscription isn't active. Please contact us to enable it or upgrade your plan.", ar: "باقتك الحالية لا تشمل هذه العملية، أو أن اشتراكك غير نشط. يرجى التواصل معنا لتفعيلها أو ترقية باقتك." },
    escalate: { en: "No — sales / ops handle plan changes.", ar: "لا — المبيعات/التشغيل يتولّون تغييرات الباقات." },
  },

  // ── Client Portal — PIN ───────────────────────────────────────────────────
  {
    code: 1203, http: 423, enumName: "PIN_UNLOCK_REQUIRED", category: "pin", severity: "guide", escalateLevel: "no",
    keywords: "1203 pin unlock required transactions page portal locked 423 enter pin يلزم إدخال رمز",
    name: { en: "PIN unlock required", ar: "يلزم إدخال رمز PIN" },
    meaning: { en: "The portal's Transactions page is PIN-protected; the user must enter their PIN before it opens.", ar: "صفحة المعاملات في البوابة محميّة برمز PIN؛ على المستخدم إدخال رمزه قبل فتحها." },
    action: { en: "Tell the portal user to enter their PIN when prompted.", ar: "أخبِر مستخدم البوابة بإدخال رمز PIN عند طلبه." },
    reply: { en: "The Transactions page is protected by a PIN. Please enter your PIN when prompted to view it.", ar: "صفحة المعاملات محميّة برمز PIN. يرجى إدخال رمزك عند طلبه لعرضها." },
    escalate: { en: "No.", ar: "لا." },
  },
  {
    code: 1204, http: 401, enumName: "INVALID_PIN", category: "pin", severity: "guide", escalateLevel: "no",
    keywords: "1204 invalid pin wrong pin reset 401 forgot رمز غير صحيح",
    name: { en: "Invalid PIN", ar: "رمز PIN غير صحيح" },
    meaning: { en: "The PIN entered for the portal Transactions page was incorrect.", ar: "رمز PIN المُدخل لصفحة المعاملات في البوابة غير صحيح." },
    action: { en: "Ask them to re-enter carefully. If forgotten, an admin can reset the PIN from the portal.", ar: "اطلب منهم إعادة الإدخال بعناية. وإذا نُسي الرمز يمكن للمشرف إعادة تعيينه من البوابة." },
    reply: { en: "That PIN was incorrect. Please try again. If you've forgotten it, we can reset it for you.", ar: "الرمز غير صحيح. يرجى المحاولة مجددًا. وإن نسيته يمكننا إعادة تعيينه لك." },
    escalate: { en: "No — an admin resets the PIN if needed.", ar: "لا — يعيد المشرف تعيين الرمز عند الحاجة." },
  },

  // ── Plan & Billing ────────────────────────────────────────────────────────
  {
    code: 1403, http: 402, enumName: "INSUFFICIENT_FUNDS", category: "billing", severity: "account", escalateLevel: "maybe",
    keywords: "1403 insufficient funds wallet balance top up prepaid 402 payment رصيد المحفظة غير كافٍ شحن",
    name: { en: "Insufficient wallet balance", ar: "رصيد المحفظة غير كافٍ" },
    meaning: { en: "The client is on prepaid billing and their wallet balance is too low to charge this transaction.", ar: "العميل على الفوترة المسبقة ورصيد محفظته منخفض جدًا لخصم هذه العملية." },
    action: { en: "Check their wallet in the admin portal. Process a top-up, or approve their pending top-up request. Loop in finance if payment is needed.", ar: "تحقّق من محفظته في بوابة الإدارة. نفّذ عملية شحن، أو اعتمد طلب الشحن المعلّق. وأشرِك المالية إذا لزم الدفع." },
    reply: { en: "Your wallet balance is too low to process this request. Please top up your wallet — or submit a top-up request from the portal — and we'll credit it.", ar: "رصيد محفظتك منخفض جدًا لمعالجة هذا الطلب. يرجى شحن محفظتك — أو إرسال طلب شحن من البوابة — وسنقوم بإضافته." },
    escalate: { en: "To finance / ops — to credit the wallet (not a bug).", ar: "إلى المالية/التشغيل — لإضافة الرصيد للمحفظة (ليست خطأً برمجيًا)." },
  },
  {
    code: 1402, http: 429, enumName: "QUOTA_EXCEEDED", category: "billing", severity: "account", escalateLevel: "no",
    keywords: "1402 quota exceeded rate limit too many requests monthly 429 تجاوز الحد الحصة",
    name: { en: "Quota / rate limit exceeded", ar: "تجاوز الحدّ/الحصة" },
    meaning: { en: "The client hit a rate limit (too many requests too fast) or their plan's usage quota.", ar: "تجاوز العميل حدّ المعدل (طلبات كثيرة بسرعة) أو حصّة الاستخدام في باقته." },
    action: { en: "Check their usage. Advise slowing the request rate and retrying shortly; route quota increases to sales/ops.", ar: "تحقّق من استخدامه. انصح بتخفيف معدّل الطلبات وإعادة المحاولة قريبًا؛ ووجّه طلبات زيادة الحصة إلى المبيعات/التشغيل." },
    reply: { en: "You've reached your usage limit. Please slow your request rate and try again shortly. If you need a higher limit, contact us to raise your quota.", ar: "لقد بلغت حدّ الاستخدام. يرجى تخفيف معدّل الطلبات والمحاولة مجددًا قريبًا. وإن احتجت حدًا أعلى فتواصل معنا لرفع حصتك." },
    escalate: { en: "No — ops / sales adjust quotas.", ar: "لا — التشغيل/المبيعات يعدّلون الحصص." },
  },
  {
    code: 1401, http: 409, enumName: "CONFLICT", category: "billing", severity: "account", escalateLevel: "maybe",
    keywords: "1401 conflict duplicate already exists state 409 تعارض مكرر",
    name: { en: "Conflict", ar: "تعارض" },
    meaning: { en: "The request conflicts with the current state — usually something that already exists (e.g. a duplicate). The message names the conflict.", ar: "يتعارض الطلب مع الحالة الحالية — غالبًا شيء موجود مسبقًا (مثل تكرار). وتوضّح الرسالة سبب التعارض." },
    action: { en: "Read the message to see what conflicts, then advise accordingly (e.g. the record already exists — no need to re-create).", ar: "اقرأ الرسالة لمعرفة سبب التعارض ثم انصح وفقًا لذلك (مثلًا السجل موجود مسبقًا — لا حاجة لإعادة إنشائه)." },
    reply: { en: "This request conflicts with existing data — it looks like the item already exists. Please review and, if needed, use the existing record.", ar: "يتعارض هذا الطلب مع بيانات موجودة — يبدو أن العنصر موجود مسبقًا. يرجى المراجعة واستخدام السجل الحالي إن لزم." },
    escalate: { en: "Only if the conflict makes no sense for what they sent — then dev with the requestId.", ar: "فقط إذا كان التعارض غير منطقي لِما أرسلوه — عندها المطوّر مع رقم الطلب (requestId)." },
  },

  // ── System & Backend ──────────────────────────────────────────────────────
  {
    code: 2103, http: 503, enumName: "CONNECTOR_UNAVAILABLE", category: "system", severity: "escalate", escalateLevel: "yes",
    keywords: "2103 connector unavailable moi down vpn tunnel service outage 503 yemen id غير متاحة انقطاع",
    name: { en: "Verification service unavailable", ar: "خدمة التحقّق غير متاحة" },
    meaning: { en: "The Yemen ID (MOI) backend is unreachable — the service is down or the VPN tunnel to it is down. This usually affects all customers at once.", ar: "تعذّر الوصول إلى خدمة الهوية اليمنية (MOI) — إمّا أن الخدمة متوقفة أو أن نفق الـ VPN إليها متوقف. وهذا يؤثّر عادةً على جميع العملاء دفعةً واحدة." },
    action: { en: "Escalate to ops immediately. Note it likely hits every client. Ops checks the VPN tunnel (moi-vpn.service) and MOI availability.", ar: "صعّد إلى التشغيل فورًا. ونبّه أنه يطال على الأرجح كل العملاء. يتحقّق التشغيل من نفق الـ VPN (moi-vpn.service) ومن توفّر MOI." },
    reply: { en: "The verification service is temporarily unavailable. Our team is already working to restore it. Please try again shortly — no action is needed on your side.", ar: "خدمة التحقّق غير متاحة مؤقتًا. فريقنا يعمل بالفعل على استعادتها. يرجى المحاولة مجددًا بعد قليل — لا يلزم أي إجراء من جهتك." },
    escalate: { en: "Yes — ops, urgent (VPN / MOI outage).", ar: "نعم — التشغيل، عاجل (انقطاع VPN / MOI)." },
  },
  {
    code: 2102, http: 504, enumName: "CONNECTOR_TIMEOUT", category: "system", severity: "escalate", escalateLevel: "yes",
    keywords: "2102 connector timeout moi slow gateway timeout 504 yemen id vpn انتهت المهلة بطيء",
    name: { en: "Verification service timed out", ar: "انتهت مهلة خدمة التحقّق" },
    meaning: { en: "The Yemen ID (MOI) backend was too slow to respond within the timeout.", ar: "كانت خدمة الهوية اليمنية (MOI) بطيئة جدًا في الرد ضمن المهلة المحدّدة." },
    action: { en: "Have them retry once. If it persists, escalate to ops to check the VPN tunnel and MOI latency.", ar: "اطلب منهم إعادة المحاولة مرة واحدة. وإن استمر، صعّد إلى التشغيل لفحص نفق الـ VPN وزمن استجابة MOI." },
    reply: { en: "The verification service is responding slowly right now. Please try again in a moment. If it keeps happening, let us know — we're monitoring it.", ar: "خدمة التحقّق تستجيب ببطء حاليًا. يرجى المحاولة مجددًا بعد لحظات. وإن استمر الأمر فأخبِرنا — نحن نراقبه." },
    escalate: { en: "Yes if it persists — ops (VPN / MOI latency).", ar: "نعم إن استمر — التشغيل (زمن استجابة VPN / MOI)." },
  },
  {
    code: 2101, http: 502, enumName: "CONNECTOR_ERROR", category: "system", severity: "escalate", escalateLevel: "yes",
    keywords: "2101 connector error moi bad response bad gateway 502 yemen id upstream خطأ في الخدمة",
    name: { en: "Verification service error", ar: "خطأ في خدمة التحقّق" },
    meaning: { en: "The Yemen ID (MOI) backend returned an error or an unexpected response we couldn't process.", ar: "أعادت خدمة الهوية اليمنية (MOI) خطأً أو استجابة غير متوقّعة تعذّر علينا معالجتها." },
    action: { en: "Escalate to dev/ops with the requestId and timestamp. Have the customer retry shortly.", ar: "صعّد إلى المطوّر/التشغيل مع رقم الطلب (requestId) والطابع الزمني. واطلب من العميل إعادة المحاولة قريبًا." },
    reply: { en: "The verification service returned a temporary error. Please try again shortly. We've logged it and are looking into it.", ar: "أعادت خدمة التحقّق خطأً مؤقتًا. يرجى المحاولة مجددًا بعد قليل. وقد سجّلناه ونحن نتابعه." },
    escalate: { en: "Yes — dev / ops with the requestId.", ar: "نعم — المطوّر/التشغيل مع رقم الطلب (requestId)." },
  },
  {
    code: 2001, http: 500, enumName: "INTERNAL_ERROR", category: "system", severity: "escalate", escalateLevel: "yes",
    keywords: "2001 internal error server bug unexpected 500 خطأ داخلي",
    name: { en: "Internal server error", ar: "خطأ داخلي في الخادم" },
    meaning: { en: "An unexpected error inside our platform — a bug or a transient fault, not something the customer did.", ar: "خطأ غير متوقّع داخل منصّتنا — خلل برمجي أو عطل مؤقّت، وليس بسبب العميل." },
    action: { en: "Capture the requestId + timestamp and escalate to dev. Ask the customer to retry.", ar: "سجّل رقم الطلب (requestId) والطابع الزمني وصعّد إلى المطوّر. واطلب من العميل إعادة المحاولة." },
    reply: { en: "A temporary error occurred on our side — nothing to fix on yours. Please try again. We've already logged it and are investigating.", ar: "حدث خطأ مؤقّت من جهتنا — لا شيء لإصلاحه لديك. يرجى المحاولة مجددًا. وقد سجّلناه ونحن نحقّق فيه." },
    escalate: { en: "Yes — dev with the requestId (always).", ar: "نعم — المطوّر مع رقم الطلب (requestId) دائمًا." },
  },
];

// UI chrome strings (bilingual). Kept here so the whole feature is one file.
export const UI = {
  title: { en: "Support — Error Playbook", ar: "الدعم — دليل رموز الأخطاء" },
  subtitle: {
    en: "Every error the Verify API returns, and what to do with it — meaning, action, the reply to send the customer, and whether to escalate.",
    ar: "كل خطأ تُرجعه واجهة التحقّق وما العمل حياله — المعنى، والإجراء، والرد المُرسَل للعميل، وهل يُصعَّد.",
  },
  search: { en: "Search a code or keyword — e.g. 1302, wallet, timeout", ar: "ابحث برمز أو كلمة — مثل 1302 أو محفظة أو مهلة" },
  exportPdf: { en: "Download PDF", ar: "تنزيل PDF" },
  codes: { en: "codes", ar: "رمز" },
  legendGuide: { en: "Guide the customer — they can fix it", ar: "أرشِد العميل — يمكنه إصلاحه" },
  legendAccount: { en: "Account / config — you or ops act", ar: "الحساب/الإعداد — أنت أو التشغيل" },
  legendEscalate: { en: "Escalate — contact dev / ops", ar: "تصعيد — تواصل مع المطوّر/التشغيل" },
  meaning: { en: "What it means", ar: "المعنى" },
  action: { en: "What to do", ar: "الإجراء" },
  reply: { en: "Reply to the customer", ar: "الرد على العميل" },
  escalate: { en: "Escalate?", ar: "تصعيد؟" },
  copy: { en: "Copy", ar: "نسخ" },
  copied: { en: "Copied", ar: "تم النسخ" },
  noResult: { en: "No error code matches your search. Try the 4-digit code or a keyword.", ar: "لا يوجد رمز خطأ يطابق بحثك. جرّب الرمز المكوّن من ٤ خانات أو كلمة مفتاحية." },
  primerTitle: { en: "Before anything — read the error", ar: "قبل أي شيء — اقرأ الخطأ" },
  envelopeTitle: { en: "Every error looks like this", ar: "كل خطأ يبدو هكذا" },
  rulesTitle: { en: "Three habits that resolve most tickets", ar: "ثلاث عادات تحلّ معظم التذاكر" },
  rule1: { en: "Grab the requestId first — it's in the body and the X-Request-Id header. Nothing can be traced without it.", ar: "خذ رقم الطلب (requestId) أولًا — موجود في جسم الرد وترويسة X-Request-Id. لا يمكن تتبّع شيء بدونه." },
  rule2: { en: "Read errorCode, not just the HTTP status — the code is precise; the status groups many causes.", ar: "اقرأ errorCode وليس فقط حالة HTTP — الرمز دقيق، بينما تجمع الحالة أسبابًا كثيرة." },
  rule3: { en: "Never accept a fingerprint image or PIN over chat/email — ask for the requestId and national number instead.", ar: "لا تقبل أبدًا صورة بصمة أو رمز PIN عبر المحادثة/البريد — اطلب رقم الطلب (requestId) والرقم الوطني بدلًا من ذلك." },
  escalationTitle: { en: "When you escalate — hand this to dev/ops", ar: "عند التصعيد — سلّم هذا للمطوّر/التشغيل" },
  escalationLead: { en: "Include every item — a ticket without a requestId can't be traced.", ar: "أدرِج كل عنصر — لا يمكن تتبّع تذكرة بدون رقم طلب (requestId)." },
  privacyTitle: { en: "Privacy — non-negotiable.", ar: "الخصوصية — غير قابلة للتفاوض." },
  privacyBody: { en: "Never collect or forward fingerprint images or PINs. The requestId lets dev pull the full (redacted) trace safely — that's all they need.", ar: "لا تجمع أو تُمرّر صور البصمات أو رموز PIN أبدًا. رقم الطلب (requestId) يتيح للمطوّر سحب السجل الكامل (المُنقّح) بأمان — وهذا كل ما يلزمهم." },
};

export const CHECKLIST: Bi[] = [
  { en: "requestId — from the response body / X-Request-Id", ar: "رقم الطلب (requestId) — من جسم الرد / ترويسة X-Request-Id" },
  { en: "Timestamp (UTC) of the failed call", ar: "الطابع الزمني (UTC) للطلب الفاشل" },
  { en: "errorCode + the full message", ar: "رمز الخطأ (errorCode) + الرسالة كاملة" },
  { en: "HTTP status returned", ar: "حالة HTTP المُرجَعة" },
  { en: "Client — tenant name or client ID (never the secret)", ar: "العميل — اسم المستأجر أو معرّف العميل (وليس السر أبدًا)" },
  { en: "Endpoint called + national number tried", ar: "نقطة النهاية المطلوبة + الرقم الوطني المُجرَّب" },
  { en: "Frequency — once, or ongoing since when", ar: "التكرار — مرة واحدة أم مستمر منذ متى" },
  { en: "Scope — one client or several", ar: "النطاق — عميل واحد أم عدة عملاء" },
];
