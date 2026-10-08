package com.middleware.platform.transactions.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Wording of the report PDFs. Status, verdict, exception and error names match
 * the portal (ar.json) and the Support playbook (portal-admin/src/data/errorPlaybook.ts),
 * so a bank sees the same words in the report, the portal and from support.
 */
final class ReportPdfLabels {

    private ReportPdfLabels() {}

    static final Map<String, String> EN = new HashMap<>();
    static final Map<String, String> AR = new HashMap<>();

    private static void both(String key, String en, String ar) {
        EN.put(key, en);
        AR.put(key, ar);
    }

    static {
        // ---- document
        both("title.summary", "Verification Activity Report", "تقرير نشاط التحقق");
        both("title.details", "Transaction Detail Report", "تقرير تفاصيل المعاملات");
        both("report.daily", "Summary by day", "ملخّص يومي");
        both("report.monthly", "Summary by month", "ملخّص شهري");
        both("report.details", "Transaction-level detail", "تفاصيل على مستوى المعاملة");
        both("range", "{from} – {to}", "من {from} إلى {to}");
        both("generatedAt", "{date}, {time} UTC", "{date}، الساعة {time} UTC");
        both("footer.confidential", "Confidential — prepared for {client}", "سري — أُعدّ لصالح {client}");
        both("footer.brand", "MOTABIQ · motabiq.ai", "مطابق · motabiq.ai");
        both("page", "Page {i} of {n}", "صفحة {i} من {n}");
        both("none", "No transactions in this period.", "لا توجد معاملات في هذه الفترة.");

        // ---- information block
        both("info.client", "Client", "العميل");
        both("info.report", "Report", "التقرير");
        both("info.period", "Period", "الفترة");
        both("info.statuses", "Statuses", "الحالات");
        both("info.generated", "Generated", "تاريخ الإصدار");
        both("info.preparedBy", "Prepared by", "أعدّه");
        both("preparedBy", "MOTABIQ — motabiq.ai", "مطابق — motabiq.ai");
        both("status.ALL", "All statuses", "جميع الحالات");
        both("status.SUCCESS", "Successful only", "الناجحة فقط");
        both("status.FAILED", "Failed only", "الفاشلة فقط");

        // ---- sections
        both("sec.summary", "Executive summary", "الملخّص التنفيذي");
        both("sec.activity", "Activity over time", "النشاط عبر الزمن");
        both("sec.results", "Verification results", "نتائج التحقق");
        both("sec.failures", "Failure analysis", "تحليل حالات الفشل");
        both("sec.perf", "Performance and billing", "الأداء والفوترة");
        both("sec.defs", "Definitions", "التعريفات");
        both("sec.transactions", "Transactions", "المعاملات");

        // ---- KPI cards
        both("kpi.requests", "Total requests", "إجمالي الطلبات");
        both("kpi.requests.days", "{n} active days", "{n} يوم نشاط");
        both("kpi.requests.months", "{n} active months", "{n} شهر نشاط");
        both("kpi.success", "Successful", "الطلبات الناجحة");
        both("kpi.success.cap", "{rate} success rate", "نسبة نجاح {rate}");
        both("kpi.failed", "Failed", "الطلبات الفاشلة");
        both("kpi.failed.cap", "Not charged", "غير محتسبة");
        both("kpi.amount", "Amount charged", "المبلغ المحتسب");
        both("kpi.amount.cap", "{n} charged requests", "{n} طلب محتسب");
        both("kpi.match", "Match rate", "نسبة المطابقة");
        both("kpi.match.cap", "{match} match · {noMatch} no match", "{match} مطابقة · {noMatch} عدم مطابقة");
        both("kpi.latency", "Average response", "متوسط زمن الاستجابة");
        both("kpi.latency.cap", "Slowest {max}", "الأبطأ {max}");
        both("kpi.busiest.daily", "Busiest day", "أكثر الأيام نشاطًا");
        both("kpi.busiest.monthly", "Busiest month", "أكثر الأشهر نشاطًا");
        both("kpi.busiest.cap", "{n} requests", "{n} طلب");

        // ---- executive summary narrative
        both("narr.main",
                "Between {from} and {to}, {client} sent {total} verification requests. {success} of them ({rate}) were completed and charged, for a total of {amount}.",
                "خلال الفترة من {from} إلى {to} أرسل {client} عدد {total} طلب تحقق، اكتمل منها {success} ({rate}) واحتُسبت بإجمالي {amount}.");
        both("narr.failed",
                " {failed} requests did not complete and were not charged; the most common reason was “{reason}” ({count}).",
                " ولم يكتمل {failed} طلبًا ولم تُحتسب، وكان السبب الأكثر تكرارًا «{reason}» ({count}).");
        both("narr.verdicts",
                " Of the completed fingerprint checks, {match} matched the national ID record and {noMatch} did not.",
                " ومن عمليات التحقق بالبصمة المكتملة تطابق {match} مع سجل الهوية الوطنية ولم يتطابق {noMatch}.");
        both("narr.none", "No verification requests were received in this period.", "لم تُستلم أي طلبات تحقق خلال هذه الفترة.");

        // ---- charts and tables
        both("chart.success", "Successful", "ناجحة");
        both("chart.failed", "Failed", "فاشلة");
        both("col.period", "Period", "الفترة");
        both("col.requests", "Requests", "الطلبات");
        both("col.success", "Successful", "ناجحة");
        both("col.failed", "Failed", "فاشلة");
        both("col.rate", "Success rate", "نسبة النجاح");
        both("col.amount", "Amount", "المبلغ");
        both("col.result", "Result", "النتيجة");
        both("col.count", "Count", "العدد");
        both("col.share", "Share", "النسبة");
        both("col.code", "Code", "الرمز");
        both("col.reason", "Reason", "السبب");
        both("total", "Total", "الإجمالي");
        both("note.results", "Share of successful requests.", "النسبة من الطلبات الناجحة.");
        both("note.failures", "Failed requests are not charged.", "الطلبات الفاشلة لا تُحتسب.");

        // ---- performance & billing
        both("perf.avg", "Average response time", "متوسط زمن الاستجابة");
        both("perf.max", "Slowest response", "أبطأ استجابة");
        both("perf.charged", "Charged requests", "الطلبات المحتسبة");
        both("perf.notCharged", "Requests not charged", "الطلبات غير المحتسبة");
        both("perf.price", "Price per charged request", "سعر الطلب المحتسب");
        both("perf.amount", "Total charged", "إجمالي المبلغ المحتسب");
        both("perf.exceptions", "Fingerprint exceptions", "استثناءات البصمة");

        // ---- definitions
        both("def.request.t", "Request", "الطلب");
        both("def.request.d", "A verification call sent by the bank to MOTABIQ.",
                "استدعاء تحقق يرسله البنك إلى منصة مطابق.");
        both("def.success.t", "Successful", "ناجح");
        both("def.success.d", "The request was processed by the national ID system and a result was returned. Successful requests are charged.",
                "عولج الطلب لدى نظام الهوية الوطنية وأُعيدت نتيجته. تُحتسب الطلبات الناجحة.");
        both("def.failed.t", "Failed / rejected", "فاشل / مرفوض");
        both("def.failed.d", "The request could not be completed — for example the fingerprint image was not acceptable, the national number was not found, or the service was unavailable. These requests are not charged.",
                "تعذّر إكمال الطلب — مثل عدم قبول صورة البصمة، أو عدم وجود الرقم الوطني، أو عدم توفر الخدمة. لا تُحتسب هذه الطلبات.");
        both("def.match.t", "Match / no match", "مطابقة / عدم مطابقة");
        both("def.match.d", "Whether the fingerprint matched the fingerprint held for that national number.",
                "ما إذا كانت البصمة تطابق البصمة المسجّلة لذلك الرقم الوطني.");
        both("def.exempt.t", "Exempt", "معفى");
        both("def.exempt.d", "The identity was checked without a fingerprint (fingerprint exception).",
                "تم التحقق من الهوية دون بصمة (استثناء البصمة).");
        both("def.notRecorded.t", "Not recorded", "غير مسجّلة");
        both("def.notRecorded.d", "Completed before result details were stored (27 September 2026).",
                "اكتملت قبل بدء تسجيل تفاصيل النتيجة (27 سبتمبر 2026).");
        both("def.latency.t", "Response time", "زمن الاستجابة");
        both("def.latency.d", "Time from receiving a request to answering it, measured for successful requests.",
                "الوقت من استلام الطلب حتى الرد عليه، ويُقاس للطلبات الناجحة.");
        both("def.time.t", "Times", "الأوقات");
        both("def.time.d", "All dates and times are in UTC.", "جميع التواريخ والأوقات بتوقيت UTC.");

        // ---- details table
        both("d.time", "Time (UTC)", "الوقت (UTC)");
        both("d.txid", "Transaction ID", "رقم المعاملة");
        both("d.type", "Type", "النوع");
        both("d.status", "Status", "الحالة");
        both("d.verdict", "Result", "النتيجة");
        both("d.code", "Code", "الرمز");
        both("d.reason", "Reason", "السبب");
        both("d.device", "Device", "الجهاز");
        both("d.nfiq", "NFIQ 2", "NFIQ 2");
        both("d.ms", "Response (ms)", "الاستجابة (ms)");
        both("d.amount", "Amount", "المبلغ");
        both("d.total", "{n} transactions · charged {amount}", "{n} معاملة · المبلغ المحتسب {amount}");
        both("type.FINGERPRINT", "Fingerprint", "بصمة");
        both("type.EXCEPTION", "Exception", "استثناء");
        both("exceptionPrefix", "Exception: ", "استثناء: ");
        both("notRecorded", "Not recorded", "غير مسجّلة");

        // ---- verdicts, statuses, exception reasons (portal wording)
        both("verdict.MATCH", "Match", "مطابقة");
        both("verdict.NO_MATCH", "No match", "عدم مطابقة");
        both("verdict.NO_VERIFICATION_POSSIBLE", "No biometric on file", "لا توجد بصمة مسجلة");
        both("verdict.EXEMPT", "Exempt (no fingerprint)", "معفى (بدون بصمة)");
        both("txStatus.SUCCESS", "Success", "ناجح");
        both("txStatus.FAILED", "Failed", "فاشل");
        both("txStatus.TIMEOUT", "Timed out", "انتهت المهلة");
        both("txStatus.REJECTED", "Rejected", "مرفوض");
        both("txStatus.INITIATED", "In progress", "مُبتدأ");
        both("exc.HAND_INJURY", "Hand injury", "إصابة في اليد");
        both("exc.AMPUTATION", "Amputation", "بتر");
        both("exc.WORN_PRINTS", "Worn prints", "بصمات متآكلة");
        both("exc.MEDICAL", "Medical", "حالة طبية");
        both("exc.OTHER", "Other", "أخرى");

        // ---- error names (Support playbook)
        both("err.1001", "Bad request", "طلب غير صحيح");
        both("err.1002", "Validation failed / invalid biometrics", "فشل التحقّق / بصمة غير صالحة");
        both("err.1003", "Fingerprint image quality rejected", "رُفضت جودة صورة البصمة");
        both("err.1004", "Fingerprint image format rejected", "رُفضت صيغة صورة البصمة");
        both("err.1101", "Authentication required", "المصادقة مطلوبة");
        both("err.1102", "Invalid credentials", "بيانات اعتماد غير صحيحة");
        both("err.1201", "Access denied (IP allow-list)", "الوصول مرفوض (قائمة IP)");
        both("err.1202", "Not entitled by plan", "غير مشمول بالباقة");
        both("err.1203", "PIN unlock required", "يلزم إدخال رمز PIN");
        both("err.1204", "Invalid PIN", "رمز PIN غير صحيح");
        both("err.1205", "Capture device not registered", "جهاز الالتقاط غير مسجّل لهذا العميل");
        both("err.1301", "National number not found", "الرقم الوطني غير موجود");
        both("err.1302", "Fingerprint did not match the ID", "البصمة لا تطابق الرقم الوطني");
        both("err.1401", "Conflict", "تعارض");
        both("err.1402", "Quota / rate limit exceeded", "تجاوز الحدّ/الحصة");
        both("err.1403", "Insufficient wallet balance", "رصيد المحفظة غير كافٍ");
        both("err.2001", "Internal server error", "خطأ داخلي في الخادم");
        both("err.2101", "Verification service error", "خطأ في خدمة التحقّق");
        both("err.2102", "Verification service timed out", "انتهت مهلة خدمة التحقّق");
        both("err.2103", "Verification service unavailable", "خدمة التحقّق غير متاحة");

        // ---- months (short / full); Arabic uses the full name for both
        String[] enShort = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String[] enFull = {"January", "February", "March", "April", "May", "June", "July", "August",
                "September", "October", "November", "December"};
        String[] ar = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس",
                "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
        for (int i = 0; i < 12; i++) {
            both("mon." + (i + 1), enShort[i], ar[i]);
            both("month." + (i + 1), enFull[i], ar[i]);
        }
    }
}
