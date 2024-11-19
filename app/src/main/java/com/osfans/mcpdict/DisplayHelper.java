package com.osfans.mcpdict;

import static com.osfans.mcpdict.DB.COL_GYHZ;
import static com.osfans.mcpdict.DB.COL_HD;
import static com.osfans.mcpdict.DB.COL_HZ;
import static com.osfans.mcpdict.DB.COL_KX;
import static com.osfans.mcpdict.DB.COL_SW;

import android.text.TextUtils;

import androidx.core.text.HtmlCompat;

import com.osfans.mcpdict.Orth.HanZi;

public abstract class DisplayHelper {
    protected static final String NULL_STRING = "-";
    private static final String PAGE_FORMAT = "(\\d+)\\.(\\d+)";
    public String mLang;

    public static CharSequence getRichText(String richTextString) {
        String s = richTextString
                .replace("\n", "<br/>")
                .replaceAll("\\*(.+?)\\*", "<b>$1</b>")
                .replaceAll("\\|(.+?)\\|", "<span style='color: #808080;'>$1</span>");
        int i = Utils.getDisplayFormat();
        if (i == 1) {
            s = s.replace("{", "<small><small>").replace("}", "</small></small>");
        } else if (i == 2) {
            s = s.replace("{", "<div class=desc>").replace("}", "</div>");
        }
        return s;
    }

    public static String formatJS(String s) {
        return s.replace("  ", "　").replace(" ", "").replace("　", " ");
    }

    public static String getRawText(String s) {
        if (TextUtils.isEmpty(s)) return "";
        return s.replaceAll("[|*\\[\\]]", "").replaceAll("\\{.*?\\}", "");
    }

    public static String normWord(String s) {
        if (TextUtils.isEmpty(s)) return "";
        StringBuilder sb = new StringBuilder();
        for (int unicode : s.codePoints().toArray()) {
            boolean isHZ = HanZi.isHz(unicode);
            if (isHZ) {
                sb.append(" ");
            }
            sb.appendCodePoint(unicode);
            if (isHZ) {
                sb.append(" ");
            }
        }
        return String.format("\"%s\"", sb.toString().trim().replace("  ", " "));
    }

    public static String normInput(String s) {
        String[] ss = s.split(" ");
        String[] newSS = new String[ss.length];
        int i = 0;
        for (String word : ss) {
            String newWord = normWord(word);
            newSS[i] = newWord;
            i++;
        }
        return String.format("'%s'", String.join(" ", newSS));
    }

    public static CharSequence formatUnknownIPA(String lang, String string) {
        StringBuilder sb = new StringBuilder();
        String s = string.replace("}\t", "}\n");
        String input = Utils.getInput();
        if (HanZi.isUnknown(input)) sb.append(s);
        else {
            String[] inputs = input.split("[, ]+");
            for (String i : s.split("\n")) {
                String i2 = i.replace(" ", "");
                for (String j: inputs) {
                    if (i2.contains(j)) {
                        sb.append(i).append("\n");
                        break;
                    }
                }
            }
        }
        return Utils.formatIPA(lang, sb.toString());
    }

    public static CharSequence formatPopUp(String hz, int i, String s) {
        if (TextUtils.isEmpty(s)) return "";
        if (i != COL_HZ) s = formatJS(s);
        if (i == COL_SW) s = s.replace("{", "<small>").replace("}", "</small>");
        else if (i == COL_KX) s = s.replaceAll(PAGE_FORMAT, "<a href=https://www.kangxizidian.com/v1/index.php?page=$1>第$1頁</a>第$2字");
        else if (i == COL_GYHZ) s = Utils.getStringRes(R.string.book_format, DB.getLanguageByLabel(DB.getColumn(i))) + s.replaceFirst(PAGE_FORMAT, "第$1頁第$2字");
        else if (i == COL_HD) s = Utils.getStringRes(R.string.book_format, DB.getLanguageByLabel(DB.getColumn(i))) + s.replaceAll(PAGE_FORMAT, "<a href=https://homeinmists.ilotus.org/hd/png/$1.png>第$1頁</a>第$2字").replace("lv", "lü").replace("nv", "nü");
        String[] fs = (s + "\n").split("\n", 2);
        String text = String.format("<p><big><big><big>%s</big></big></big> %s</p><br><p>%s</p>", hz, fs[0], fs[1].replace("\n", "<br/>"));
        return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT);
    }

    public boolean isIPA(char c) {
        int type = Character.getType(c);
        if (HanZi.isHz(c)) return false;
        return Character.isLetterOrDigit(c)
                || type == Character.NON_SPACING_MARK
                || type == Character.MODIFIER_SYMBOL
                || type == Character.OTHER_NUMBER;
    }

    public String display(String s) {
        if (s == null) return NULL_STRING;
        s = lineBreak(s);
        // Find all regions of [a-z0-9]+ in s, and apply display helper to each of them
        StringBuilder sb = new StringBuilder();
        int L = s.length(), p = 0;
        boolean isMeaning;
        boolean isLang = DB.isLang(mLang);
        String js;
        while (p < L) {
            int q = p;
            while (q < L && isIPA(s.charAt(q))) q++;
            if (q > p) {
                String t1 = s.substring(p, q);
                String t2 = displayOne(t1);
                sb.append(TextUtils.isEmpty(t2) ? t1 : t2);
                p = q;
            }
            isMeaning = false;
            while (p < L && (isMeaning || !isIPA(s.charAt(p)))) {
                if (s.charAt(p) == '{') isMeaning = true;
                else if (s.charAt(p) == '}') isMeaning = false;
                p++; //
            }
            js = s.substring(q, p);
            if (isLang) js = formatJS(js);
            sb.append(js);
        }
        // Add spaces as hints for line wrapping
        s = sb.toString().replace("\t", " ").replace(",", " ").replace("  ", " ")
                .replace("(", " (")
                .replace("]", "] ")
                .trim();
        return s;
    }

    public String display(String s, String lang) {
        mLang = lang;
        return display(s);
    }

    public CharSequence displayRich(String s, String lang) {
        return getRichText(display(s, lang));
    }

    public String getLang() {
        return mLang;
    }

    public String lineBreak(String s) {
        return s;
    }

    public abstract String displayOne(String s);
}