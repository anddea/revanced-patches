package app.morphe.extension.youtube.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class LatexMarkdownTest {
    @Test
    public void recognizesAllGeminiMathDelimiters() {
        String text = "$x^2$ and $$\\frac{12}{0.8}=15\\text{ m}$$ then \\(\\tan\\theta\\) and \\[a_b\\]";
        var expressions = LatexMarkdown.findExpressions(text);
        assertEquals(4, expressions.size());
        assertEquals("x^2", expressions.get(0).latex());
        assertFalse(expressions.get(0).block());
        assertEquals("\\frac{12}{0.8}=15\\text{ m}", expressions.get(1).latex());
        assertTrue(expressions.get(1).block());
        assertEquals("\\tan\\theta", expressions.get(2).latex());
        assertFalse(expressions.get(2).block());
        assertEquals("a_b", expressions.get(3).latex());
        assertTrue(expressions.get(3).block());
        for (var expression : expressions) {
            assertTrue(expression.end() > expression.start());
            assertTrue(text.substring(expression.start(), expression.end()).contains(expression.latex()));
        }
    }

    @Test
    public void leavesCodeEscapedDollarsAndCurrencyLiteral() {
        String text = "`$a$` ``$b$``\n```latex\n$c$\n```\n~~~\n$d$\n~~~\n\\$5 and $20 or $30";
        assertTrue(LatexMarkdown.findExpressions(text).isEmpty());
    }

    @Test
    public void doesNotRenderUnfinishedStreamOrCodeFence() {
        assertTrue(LatexMarkdown.findExpressions("text $$\\frac{1}{2}").isEmpty());
        assertTrue(LatexMarkdown.findExpressions("text $$x + $a$").isEmpty());
        assertTrue(LatexMarkdown.findExpressions("text $x").isEmpty());
        assertTrue(LatexMarkdown.findExpressions("text \\[x").isEmpty());
        assertTrue(LatexMarkdown.findExpressions("```\n$x$ then $y$").isEmpty());
    }

    @Test
    public void allowsEscapedDollarsInsideMathAndMultilineDisplayMath() {
        var expressions = LatexMarkdown.findExpressions("$\\text{cost} = \\$5$\n$$\na_b * c\n$$");
        assertEquals(2, expressions.size());
        assertEquals("\\text{cost} = \\$5", expressions.get(0).latex());
        assertEquals("\na_b * c\n", expressions.get(1).latex());
    }

    @Test
    public void handlesTheFormulasFromAiStudioResponse() {
        String text = "**Calculation:** ($\\frac{\\text{Tree Height}}{\\text{Tree Shadow}} = "
                + "\\frac{\\text{Stick Height}}{\\text{Stick Shadow}}$), a $1\\text{ m}$ stick.\n"
                + "$$\\tan(\\theta) = \\frac{\\text{opposite leg}}{\\text{adjacent leg}}$$\n"
                + "[00:10 – 00:30]";
        var expressions = LatexMarkdown.findExpressions(text);
        assertEquals(3, expressions.size());
        assertTrue(expressions.get(2).block());
        assertFalse(expressions.get(2).latex().contains("00:10"));
    }
}
