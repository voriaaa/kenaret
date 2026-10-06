# Source of truth for advice content. Run `python3 tools/advice_content.py` to regenerate
# app/src/main/res/values*/strings_advice.xml and content/AdviceCatalog.kt.
import os
from xml.sax.saxutils import escape

M, F, O, L = "MENSTRUAL", "FOLLICULAR", "OVULATION", "LUTEAL"
SC, ES, PS, CO, ED = "SELF_CARE", "EMOTIONAL_SUPPORT", "PRACTICAL_SUPPORT", "COMMUNICATION", "EDUCATION"

HER = [
 (M, SC, "استراحت بدون عذاب وجدان", "اگر امروز بدنت انرژی کمتری دارد، کمی استراحت بیشتر اشکالی ندارد. برنامه‌ات را تا جایی که می‌شود سبک‌تر بچین.",
  "Rest without guilt", "If your body has less energy today, resting a bit more is okay. Lighten your plans where you can."),
 (M, SC, "گرما می‌تواند کمک کند", "برای بعضی افراد، کیسه آب گرم یا دوش گرم در روزهای پریود حس بهتری می‌دهد. ببین برای تو چطور است.",
  "Warmth can help", "For some people, a hot water bottle or a warm shower feels good during their period. See what works for you."),
 (M, SC, "آب کافی بنوش", "نوشیدن آب و مایعات گرم در طول روز ممکن است به احساس بهتر کمک کند.",
  "Stay hydrated", "Drinking water and warm drinks throughout the day may help you feel better."),
 (M, CO, "نیازت را بگو", "اگر امروز به کمک یا فضای بیشتری نیاز داری، گفتنش به آدم‌های نزدیکت می‌تواند همه‌چیز را ساده‌تر کند.",
  "Say what you need", "If you need help or more space today, telling the people close to you can make things easier."),
 (M, ED, "تجربه هر کس متفاوت است", "شدت و مدت پریود از فردی به فرد دیگر و حتی از ماهی به ماه دیگر می‌تواند فرق کند. مقایسه با دیگران لازم نیست.",
  "Everyone's experience differs", "Flow and length can vary from person to person, and even from month to month. There is no need to compare."),
 (M, SC, "حرکت ملایم، اگر دوست داری", "برای بعضی‌ها پیاده‌روی کوتاه یا کشش آرام حال بهتری می‌آورد؛ اگر حوصله‌اش را نداری، استراحت هم انتخاب خوبی است.",
  "Gentle movement, if you like", "Some people feel better after a short walk or gentle stretching. If you're not up for it, resting is a good choice too."),
 (M, ED, "علائمت را ثبت کن", "ثبت علائم در چند چرخه کمک می‌کند الگوی بدن خودت را بهتر بشناسی و در صورت نیاز با پزشک دقیق‌تر صحبت کنی.",
  "Log your symptoms", "Logging symptoms over a few cycles helps you learn your own patterns and talk to a doctor more clearly if needed."),
 (M, ED, "درد شدید را جدی بگیر", "اگر درد آن‌قدر شدید است که کارهای روزمره‌ات را مختل می‌کند، صحبت با پزشک یا متخصص سلامت ایده خوبی است.",
  "Take strong pain seriously", "If pain is strong enough to disrupt your daily life, it's a good idea to talk with a doctor or health professional."),
 (F, SC, "زمان مناسب برای شروع‌های کوچک", "بعضی افراد در این روزها انرژی بیشتری حس می‌کنند. اگر برای تو هم این‌طور است، شاید وقت خوبی برای شروع یک عادت کوچک باشد.",
  "A good time for small starts", "Some people notice more energy in these days. If you do too, it might be a nice time to start a small habit."),
 (F, ES, "به خودت اعتبار بده", "کارهایی را که این هفته انجام دادی، حتی کوچک، ببین. توجه به پیشرفت‌ها حال خوبی دارد.",
  "Give yourself credit", "Notice what you've done this week, even the small things. Paying attention to progress feels good."),
 (F, CO, "برنامه‌ریزی مشترک", "اگر دوست داری، با پارتنرت برای یک فعالیت یا قرار مشترک در روزهای آینده برنامه بریز.",
  "Plan something together", "If you'd like, plan an activity or a date with your partner for the coming days."),
 (F, SC, "وعده‌های منظم", "وعده‌های متنوع و منظم کمک می‌کنند انرژی روزانه‌ات پایدارتر بماند. لازم نیست کامل باشد؛ همین که منظم باشد کافی است.",
  "Regular meals", "Regular, varied meals can help keep your energy steady. They don't have to be perfect, just regular."),
 (F, SC, "فعالیتی که دوستش داری", "اگر حال و انرژی‌اش را داری، فعالیت بدنی دلخواهت را امتحان کن؛ چیزی که از آن لذت ببری، نه اجبار.",
  "Movement you enjoy", "If you feel like it, try a physical activity you enjoy: something fun, not an obligation."),
 (F, ED, "مرحله فولیکولار چیست؟", "این مرحله معمولاً از پایان پریود تا نزدیکی تخمک‌گذاری است. طول آن در افراد مختلف متفاوت است و همین یکی از دلایل تفاوت طول چرخه‌هاست.",
  "What is the follicular phase?", "It usually runs from the end of your period until around ovulation. Its length varies between people, which is one reason cycle lengths differ."),
 (F, ES, "وقتی برای خودت", "کمی زمان فقط برای خودت کنار بگذار؛ کتاب، موسیقی یا هر چیزی که آرامت می‌کند.",
  "Time for yourself", "Set aside a little time just for you: a book, music, or anything that calms you."),
 (O, ED, "بازه احتمالی، نه روز قطعی", "کنارت فقط یک بازه تخمینی برای تخمک‌گذاری نشان می‌دهد. زمان واقعی ممکن است متفاوت باشد و این برنامه روش پیشگیری از بارداری نیست.",
  "An estimated window, not a fixed day", "Kenaret only shows an estimated ovulation window. The real timing can differ, and this app is not a method of contraception."),
 (O, ED, "نشانه‌های بدن", "بعضی افراد در این روزها تغییراتی در ترشحات یا حس بدن متوجه می‌شوند و بعضی هیچ تغییری حس نمی‌کنند. هر دو طبیعی است.",
  "Body signals", "Some people notice changes in discharge or in how their body feels around now, and some notice nothing. Both are normal."),
 (O, SC, "خواب منظم", "سعی کن ساعت خوابت تا حد امکان منظم بماند؛ خواب کافی روی انرژی و حالت در تمام چرخه اثر دارد.",
  "Regular sleep", "Try to keep a regular sleep schedule. Good sleep affects your energy and mood across the whole cycle."),
 (O, CO, "گفت‌وگوی باز", "اگر موضوعی درباره بدنت یا رابطه‌تان ذهنت را مشغول کرده، صحبت آرام و صادقانه با پارتنرت می‌تواند کمک کند.",
  "Open conversation", "If something about your body or your relationship is on your mind, a calm and honest talk with your partner can help."),
 (O, ES, "حال امروزت را بپذیر", "هر حالی که امروز داری قابل احترام است. لازم نیست خودت را با تصویر خاصی از این مرحله تطبیق بدهی.",
  "Accept how you feel today", "However you feel today is valid. You don't have to match any particular idea of this phase."),
 (O, SC, "یک پیاده‌روی کوتاه", "اگر فرصتش هست، چند دقیقه پیاده‌روی در هوای آزاد می‌تواند حال روزت را تازه کند.",
  "A short walk", "If you have a moment, a few minutes walking outside can refresh your day."),
 (L, SC, "سبک‌تر برنامه بریز", "برای بعضی افراد روزهای قبل از پریود کمی سنگین‌تر است. اگر برای تو هم این‌طور است، از قبل کارهایت را سبک‌تر بچین.",
  "Plan a lighter week", "For some people the days before a period feel heavier. If that's true for you, plan a lighter schedule ahead of time."),
 (L, SC, "از قبل آماده باش", "وسایلی را که معمولاً در روزهای پریود لازم داری از حالا کنار دستت بگذار تا غافلگیر نشوی.",
  "Get ready ahead", "Keep the things you usually need for your period within reach now, so you're not caught off guard."),
 (L, ES, "مهربان‌تر با خودت", "اگر این روزها حوصله کمتری داری، با خودت سخت نگیر. تجربه هر فرد متفاوت است و هر حسی قابل احترام است.",
  "Be kinder to yourself", "If you have less patience these days, go easy on yourself. Everyone's experience is different, and every feeling is valid."),
 (L, SC, "نمک و کافئین را زیر نظر بگیر", "بعضی‌ها متوجه می‌شوند کم کردن نمک یا کافئین در این روزها حس نفخ یا بی‌قراری را کمتر می‌کند. ببین برای تو چطور است.",
  "Notice salt and caffeine", "Some people find that less salt or caffeine in these days eases bloating or restlessness. See how it is for you."),
 (L, CO, "از قبل بگو", "اگر می‌دانی چه چیزی در روزهای پیش رو کمکت می‌کند، از قبل با پارتنرت در میان بگذار تا مجبور به حدس زدن نباشد.",
  "Tell them in advance", "If you know what helps you in the coming days, share it with your partner ahead of time so they don't have to guess."),
 (L, SC, "خواب کافی", "در این روزها بعضی افراد به خواب بیشتری نیاز دارند. اگر می‌شود، کمی زودتر به رختخواب برو.",
  "Enough sleep", "Some people need more sleep around now. If you can, go to bed a little earlier."),
 (L, ES, "یادداشت حال", "نوشتن چند خط درباره حس امروزت می‌تواند ذهنت را سبک‌تر کند و بعداً الگوهایت را نشانت بدهد.",
  "Mood notes", "Writing a few lines about how you feel can lighten your mind and later show you your own patterns."),
 (L, ED, "مرحله لوتئال چیست؟", "این مرحله معمولاً از بعد از تخمک‌گذاری تا شروع پریود بعدی است. علائم آن برای هر فرد متفاوت است و بعضی‌ها اصلاً علامتی ندارند.",
  "What is the luteal phase?", "It usually runs from after ovulation until the next period. Symptoms differ for everyone, and some people have none at all."),
 (L, ED, "وقتی تغییرها زیاد است", "اگر علائم قبل از پریود زندگی روزمره‌ات را به‌شدت تحت تأثیر قرار می‌دهد، مشورت با پزشک یا متخصص سلامت می‌تواند کمک کند.",
  "When changes feel big", "If symptoms before your period strongly affect daily life, talking with a doctor or health professional can help."),
]

PARTNER = [
 (M, CO, "بپرس، حدس نزن", "به جای اینکه حدس بزنی چه احساسی دارد، از خودش بپرس: «امروز چه چیزی کمکت می‌کند؟»",
  "Ask, don't guess", "Instead of guessing how she feels, ask her: “What would help you today?”"),
 (M, PS, "کارهای کوچک روزمره", "یک کار کوچک مثل آماده کردن نوشیدنی گرم، شستن ظرف‌ها یا خرید روزانه را داوطلبانه انجام بده.",
  "Small everyday tasks", "Volunteer for something small, like making a warm drink, doing the dishes or the daily shopping."),
 (M, PS, "وسایل لازم", "اگر می‌دانی در این روزها تهیه بعضی وسایل برایش سخت است، می‌توانی داوطلبانه کمک کنی؛ فقط قبلش بپرس چه چیزی لازم دارد.",
  "Supplies", "If getting certain things is harder for her these days, you can offer to help. Just ask first what she needs."),
 (M, ES, "درد را جدی بگیر", "اگر می‌گوید درد دارد، حرفش را باور کن و کوچکش نشمار. همراهی ساده گاهی از هر راه‌حلی مهم‌تر است.",
  "Take pain seriously", "If she says she's in pain, believe her and don't downplay it. Simply being there often matters more than any solution."),
 (M, ES, "فضای استراحت", "اگر خسته است، فضای استراحت را برایش فراهم کن؛ مثلاً محیط را آرام نگه دار یا بخشی از کارها را خودت انجام بده.",
  "Room to rest", "If she's tired, make room for rest: keep things calm or take over some tasks yourself."),
 (M, ED, "تجربه هر فرد فرق دارد", "پریود برای هر فرد و حتی در هر ماه می‌تواند متفاوت باشد. چیزی که ماه پیش لازم بود، شاید این ماه لازم نباشد.",
  "Every experience is different", "Periods differ between people and even from month to month. What helped last month might not be needed this month."),
 (M, CO, "بدون شوخی درباره پریود", "شوخی درباره پریود یا علائمش، حتی با نیت خوب، ممکن است ناخوشایند باشد. احترام همیشه انتخاب امن‌تری است.",
  "Skip period jokes", "Jokes about periods or symptoms, even well-meant ones, can feel hurtful. Respect is always the safer choice."),
 (M, PS, "برنامه‌ها را منعطف کن", "اگر برنامه‌ای داشتید و حالش مساعد نیست، بدون دلخوری پیشنهاد بده که برنامه را عقب بیندازید یا ساده‌ترش کنید.",
  "Keep plans flexible", "If you had plans and she isn't feeling up to it, offer to postpone or simplify without making it a big deal."),
 (F, CO, "وقت با هم", "اگر هر دو حوصله‌اش را دارید، برای یک فعالیت مشترک یا قرار دونفره برنامه بریزید. از او بپرس چه چیزی دوست دارد.",
  "Time together", "If you're both up for it, plan an activity or a date together. Ask her what she'd enjoy."),
 (F, ES, "حمایت از برنامه‌هایش", "اگر هدف یا پروژه تازه‌ای را شروع کرده، علاقه نشان بده و بپرس چطور می‌توانی پشتیبانش باشی.",
  "Back her plans", "If she's starting a new goal or project, show interest and ask how you can support her."),
 (F, ED, "مرحله فولیکولار", "این مرحله معمولاً بعد از پریود است. بعضی افراد انرژی بیشتری حس می‌کنند، ولی این برای همه یکسان نیست؛ فرض نکن، بپرس.",
  "The follicular phase", "This phase usually follows the period. Some people feel more energetic, but not everyone does. Don't assume, ask."),
 (F, CO, "گفت‌وگوی بدون عجله", "زمانی را برای صحبت آرام و بدون عجله کنار بگذار؛ درباره روزش، برنامه‌هایش یا هر چیزی که دوست دارد.",
  "Unhurried talks", "Make time for a calm, unhurried conversation about her day, her plans or anything she wants to talk about."),
 (F, PS, "تقسیم منصفانه کارها", "حمایت فقط مخصوص روزهای پریود نیست. تقسیم منصفانه کارهای خانه در تمام ماه یعنی همراهی واقعی.",
  "Share chores fairly", "Support isn't only for period days. Sharing housework fairly all month long is real partnership."),
 (F, ES, "قدردانی کن", "یک جمله ساده قدردانی برای کاری که انجام داده یا برای خودِ او، می‌تواند روزش را بهتر کند.",
  "Show appreciation", "A simple word of appreciation for something she did, or for who she is, can brighten her day."),
 (F, ED, "چرخه فقط پریود نیست", "چرخه قاعدگی چند مرحله دارد و پریود فقط بخشی از آن است. شناخت کل چرخه به همدلی بیشتر کمک می‌کند.",
  "The cycle is more than the period", "The menstrual cycle has several phases, and the period is only one of them. Understanding the whole cycle builds empathy."),
 (O, ED, "فقط یک تخمین", "کنارت فقط یک بازه احتمالی برای تخمک‌گذاری نشان می‌دهد. این تخمین برای پیشگیری از بارداری قابل اتکا نیست.",
  "Only an estimate", "Kenaret shows only an estimated ovulation window. It is not reliable for preventing pregnancy."),
 (O, CO, "احترام به انتخاب‌ها", "تصمیم‌ها درباره بدن و رابطه، مشترک و با رضایت کامل هر دو نفر است. همیشه بپرس و به پاسخ احترام بگذار.",
  "Respect choices", "Decisions about bodies and intimacy are shared and need full consent from both of you. Always ask and respect the answer."),
 (O, ES, "فرض نکن", "هیچ مرحله‌ای از چرخه تعیین نمی‌کند او چه حسی دارد. حال هر روزش را از خودش بشنو، نه از تقویم.",
  "Don't assume", "No phase decides how she feels. Hear about her day from her, not from a calendar."),
 (O, PS, "یک غافلگیری کوچک", "یک کار کوچک و غیرمنتظره، مثل آماده کردن غذای مورد علاقه‌اش یا یک یادداشت محبت‌آمیز، می‌تواند حس خوبی بدهد.",
  "A small surprise", "Something small and unexpected, like her favourite meal or a kind note, can feel lovely."),
 (O, CO, "گوش دادن فعال", "وقتی صحبت می‌کند، گوشی را کنار بگذار و کامل گوش بده. همیشه لازم نیست راه‌حل بدهی.",
  "Active listening", "When she talks, put your phone down and really listen. You don't always need to offer a solution."),
 (O, ED, "هر بدن متفاوت است", "بعضی افراد در این روزها تغییری حس می‌کنند و بعضی هیچ تغییری. کنجکاو بودن بهتر از نتیجه‌گیری است.",
  "Every body is different", "Some people notice changes around now and some don't notice any. Curiosity beats conclusions."),
 (L, CO, "یک سؤال ساده", "امروز می‌توانی با یک سؤال ساده شروع کنی: «چیزی هست که بتوانم کمکت کنم؟»",
  "One simple question", "Today you could start with a simple question: “Is there anything I can help with?”"),
 (L, ES, "قضاوت نکن", "اگر حال و حوصله‌اش متفاوت است، درباره احساسش قضاوت نکن و آن را به هورمون‌ها نسبت نده. احساس هر کس دلایل خودش را دارد.",
  "Don't judge", "If her mood is different, don't judge it or blame hormones. Every feeling has its own reasons."),
 (L, PS, "آماده‌سازی از قبل", "اگر پریود نزدیک است، بپرس آیا وسیله‌ای لازم دارد که از قبل تهیه شود.",
  "Prepare ahead", "If her period may be coming soon, ask whether there's anything she'd like to have ready."),
 (L, PS, "بار ذهنی را کم کن", "مسئولیت چند کار برنامه‌ریزی‌شده مثل خرید یا پرداخت قبض‌ها را کامل به عهده بگیر تا بار ذهنی کمتری داشته باشد.",
  "Lighten the mental load", "Fully take over a few planned tasks, like shopping or paying bills, so she has less to keep track of."),
 (L, ES, "فضای شخصی", "اگر به فضای شخصی نیاز دارد، آن را محترم بشمار. فاصله گرفتن موقت به معنی دلخوری نیست.",
  "Personal space", "If she needs personal space, respect it. Taking some time alone doesn't mean something is wrong between you."),
 (L, CO, "اختلاف را آرام حل کن", "اگر بحثی پیش آمد، روی خودِ موضوع تمرکز کن و از جمله‌هایی مثل «حتماً به خاطر پریودت است» دوری کن.",
  "Handle disagreements calmly", "If you disagree, focus on the issue itself and avoid lines like “It must be your period.”"),
 (L, ED, "مرحله لوتئال", "این مرحله معمولاً بین تخمک‌گذاری و پریود بعدی است. بعضی افراد علائمی مثل خستگی یا نفخ دارند و بعضی هیچ علامتی ندارند.",
  "The luteal phase", "This phase usually falls between ovulation and the next period. Some people have symptoms like tiredness or bloating, others have none."),
 (L, PS, "خواب آرام", "اگر به خواب بیشتری نیاز دارد، محیط را آرام نگه دار یا کارهای صبح را خودت انجام بده.",
  "Restful sleep", "If she needs more sleep, keep things quiet or handle the morning tasks yourself."),
 (L, ES, "حضورت مهم است", "گاهی بهترین حمایت فقط بودن است: یک فیلم با هم، یک چای گرم یا نشستن کنار هم بدون حرف.",
  "Being there matters", "Sometimes the best support is just being there: a film together, a warm tea, or sitting side by side in silence."),
]

def res_escape(s):
    s = escape(s)
    return s.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')

def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    res = os.path.join(root, "app/src/main/res")
    assert len(HER) >= 30 and len(PARTNER) >= 30
    items = [("her", i + 1, "HER", a) for i, a in enumerate(HER)] + [("partner", i + 1, "PARTNER", a) for i, a in enumerate(PARTNER)]
    for lang, ti, bi, folder in (("fa", 2, 3, "values"), ("en", 4, 5, "values-en")):
        lines = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- Generated by tools/advice_content.py. Do not edit by hand. -->", "<resources>"]
        for prefix, n, _, a in items:
            key = f"advice_{prefix}_{n:02d}"
            lines.append(f'    <string name="{key}_title">{res_escape(a[ti])}</string>')
            lines.append(f'    <string name="{key}_body">{res_escape(a[bi])}</string>')
        lines.append("</resources>\n")
        os.makedirs(os.path.join(res, folder), exist_ok=True)
        open(os.path.join(res, folder, "strings_advice.xml"), "w", encoding="utf-8").write("\n".join(lines))

    kt = ["package com.voria.kenaret.content", "",
          "import com.voria.kenaret.R",
          "import com.voria.kenaret.domain.Advice",
          "import com.voria.kenaret.domain.AdviceCategory",
          "import com.voria.kenaret.domain.Audience",
          "import com.voria.kenaret.domain.CyclePhase", "",
          "/** Generated by tools/advice_content.py. Text lives in res/values/strings_advice.xml (+ values-en). */",
          "internal object AdviceData {",
          "    val all: List<Advice> = listOf("]
    for prefix, n, aud, a in items:
        key = f"advice_{prefix}_{n:02d}"
        kt.append(f'        Advice("{key}", CyclePhase.{a[0]}, Audience.{aud}, AdviceCategory.{a[1]}, R.string.{key}_title, R.string.{key}_body),')
    kt += ["    )", "}", ""]
    open(os.path.join(root, "app/src/main/java/com/voria/kenaret/content/AdviceData.kt"), "w", encoding="utf-8").write("\n".join(kt))
    print("her", len(HER), "partner", len(PARTNER))

if __name__ == "__main__":
    main()
