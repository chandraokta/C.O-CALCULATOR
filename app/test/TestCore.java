import com.kalku.sc100.*;
import java.math.BigDecimal;
public class TestCore {
  public static void main(String[] a) throws Exception {
    CalcEngine.Context c = new CalcEngine.Context(CalcEngine.Angle.DEG, BigDecimal.ZERO);
    String[] es = {"1/3","1234567890123","0.000012345","200+10%","sin(30)","cos(60)","tan(45)","asin(0.5)","ln(e)","log(1000)","5 nCr 2","5 nPr 2","10!","2^0.5","8 mod 5","-8 mod 5","8 mod -5","1.2e3+1","2π","√(25)"};
    for(String e: es){ try { BigDecimal v=CalcEngine.eval(e,c); System.out.println(e+" -> "+v.toPlainString()); } catch(Exception ex){ System.out.println(e+" !! "+ex.getMessage()); } }
    BigDecimal[] vs={new BigDecimal("1234.5678"),new BigDecimal("0.000012345"),new BigDecimal("1234567890123"),new BigDecimal("1").divide(new BigDecimal("3"),java.math.MathContext.DECIMAL128)};
    for(BigDecimal v: vs){ System.out.println("AUTO "+Formatter.format(v,Formatter.Mode.AUTO,4,true)); System.out.println("FIX  "+Formatter.format(v,Formatter.Mode.FIX,4,true)); System.out.println("SCI  "+Formatter.format(v,Formatter.Mode.SCI,6,true)); System.out.println("ENG  "+Formatter.format(v,Formatter.Mode.ENG,6,true)); }
    UnitConverter.Category len=UnitConverter.CATEGORIES.get("LENGTH");
    System.out.println("1 m -> ft = "+UnitConverter.convert(len,0,8,1));
    UnitConverter.Category t=UnitConverter.CATEGORIES.get("TEMP");
    System.out.println("0 C -> F = "+UnitConverter.convert(t,0,1,0));
    System.out.println("100 C -> K = "+UnitConverter.convert(t,0,2,100));
    System.out.println("1 GB -> GiB = "+UnitConverter.convert(UnitConverter.CATEGORIES.get("DATA"),6,7,1));
  }
}
