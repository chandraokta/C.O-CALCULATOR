import com.kalku.sc100.*;
import java.math.BigDecimal;
public class TestUnicode {
  public static void main(String[] a) throws Exception {
    CalcEngine.Context c = new CalcEngine.Context(CalcEngine.Angle.DEG, BigDecimal.ZERO);
    String[] es = {"\u221A(25)", "\u221B(27)", "2\u03C0", "sin(30)", "cos(60)", "tan(45)"};
    for(String e: es){ System.out.println(e+" -> "+CalcEngine.eval(e,c)); }
    System.out.println(Formatter.format(new BigDecimal("1.2345"), Formatter.Mode.FIX, 4, true));
    System.out.println(Formatter.format(new BigDecimal("1234.5"), Formatter.Mode.FIX, 2, true));
    System.out.println(UnitConverter.formatValue(3.280839895));
    System.out.println(UnitConverter.formatValue(1.057e-16));
  }
}
