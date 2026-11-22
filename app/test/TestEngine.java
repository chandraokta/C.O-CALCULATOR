import com.kalku.sc100.CalcEngine;
import java.math.BigDecimal;
public class TestEngine {
  static void t(String e, String want) throws Exception {
    CalcEngine.Context c = new CalcEngine.Context(CalcEngine.Angle.DEG, BigDecimal.ZERO);
    BigDecimal v = CalcEngine.eval(e, c);
    System.out.println(e + " = " + v.toPlainString() + (want==null?"":"  [want " + want + "]"));
  }
  public static void main(String[] a) throws Exception {
    t("1+2*3", "7"); t("(1+2)*3", "9"); t("2^3^2", "512"); t("-2^2", "-4");
    t("200+10%", "220"); t("200-10%", "180"); t("200*10%", "20"); t("200/10%", "2000");
    t("sin(30)", "0.5"); t("cos(60)", "0.5"); t("5 nCr 2", "10"); t("5 nPr 2", "20");
    t("10!", "3628800"); t("2π", null); t("√(25)", "5"); t("∛(27)", "3");
    t("ln(e)", "1"); t("log(1000)", "3"); t("8 mod 5", "3"); t("1.2e3+1", "1201");
    t("2 sin(30)", "1"); t("(2)(3)", "6"); t("50%", "0.5"); t("1/3", null);
  }
}
