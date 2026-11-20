package app.morphe.extension.youtube.utils;

import java.util.ArrayList;
import java.util.List;

/** Finds complete math expressions before Markdown can interpret LaTeX underscores or asterisks. */
final class LatexMarkdown {
    record Expression(int start, int end, String latex, boolean block) { }

    private LatexMarkdown() { }

    /** Code spans/fences, escaped delimiters and unfinished streamed expressions remain literal. */
    static List<Expression> findExpressions(String text) {
        List<Expression> expressions = new ArrayList<>();
        for (int i = 0; i < text.length();) {
            char c = text.charAt(i);
            if (!escaped(text, i) && (c == '`' || (c == '~' && text.startsWith("~~~", i)))) {
                int end = i + 1;
                while (end < text.length() && text.charAt(end) == c) end++;
                String fence = text.substring(i, end);
                int closing = text.indexOf(fence, end);
                i = closing < 0 ? text.length() : closing + fence.length();
                continue;
            }
            if (escaped(text, i)) {
                i++;
                continue;
            }
            String closing;
            int openingLength;
            boolean block;
            if (c == '$') {
                block = text.startsWith("$$", i);
                openingLength = block ? 2 : 1;
                closing = block ? "$$" : "$";
            } else if (text.startsWith("\\(", i) || text.startsWith("\\[", i)) {
                block = text.charAt(i + 1) == '[';
                openingLength = 2;
                closing = block ? "\\]" : "\\)";
            } else {
                i++;
                continue;
            }
            int contentStart = i + openingLength;
            int end = text.indexOf(closing, contentStart);
            while (end >= 0 && escaped(text, end)) end = text.indexOf(closing, end + closing.length());
            if (end < 0) {
                // Do not reinterpret the second dollar of an unfinished display expression.
                if (block) break;
                i += openingLength;
                continue;
            }
            String latex = text.substring(contentStart, end);
            boolean inlineDollar = c == '$' && !block;
            if (latex.trim().isEmpty() || (inlineDollar && (latex.indexOf('\n') >= 0
                    || Character.isWhitespace(latex.charAt(0))
                    || Character.isWhitespace(latex.charAt(latex.length() - 1))
                    || (end + 1 < text.length() && Character.isDigit(text.charAt(end + 1)))))) {
                i += openingLength;
                continue;
            }
            int expressionEnd = end + closing.length();
            expressions.add(new Expression(i, expressionEnd, latex, block));
            i = expressionEnd;
        }
        return expressions;
    }

    private static boolean escaped(String text, int index) {
        int backslashes = 0;
        while (index > 0 && text.charAt(--index) == '\\') backslashes++;
        return (backslashes & 1) != 0;
    }
}
