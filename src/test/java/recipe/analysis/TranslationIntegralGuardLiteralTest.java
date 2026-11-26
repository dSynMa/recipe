package recipe.analysis;
import org.junit.Test;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.predicate.GuardReference;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.store.ConcreteStore;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Guard;
import recipe.lang.types.Real;
import java.util.HashMap;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;
public class TranslationIntegralGuardLiteralTest {
 private String model(String type,String value) throws Exception {
  return source("observation-forall-constant")
   .replace("agent Sender", "guard same(n: "+type+", m: 0..1) := n == m;\nagent Sender")
   .replace("local: sent: boolean", "local: sent: boolean, score: 0..1")
   .replace("init: !sent", "init: !sent & score == 1")
   .replace("send: <TRUE>", "send: <same("+value+", score)>");
 }
 @Test public void integralDecimalFitsABoundedIntegerGuardParameter() throws Exception {
  for(String type:new String[]{"0..1","integer"}) for(String value:new String[]{"1", "1.0", "0.0 + 1.0"})
   assertCtl(ToNuXmv.transform(parse(model(type,value))),"EX receiver-received",true);
 }
 @Test public void directConstructionAcceptsAndNormalizesIntegralLiterals() throws Exception {
  parse(model("0..1","1"));
  Guard type=Guard.getDefinition("same").getType();
  GuardReference call=new GuardReference(type,new Expression[]{new TypedValue(Real.getType(),"1.0"),
    new TypedValue(new BoundedInteger(0,1),"1")});
  assertEquals(Condition.getTrue(),call.valueIn(new ConcreteStore(new HashMap<>())));
 }
 @Test public void fractionalAndOutOfBoundsLiteralsRemainRejected() throws Exception {
  for(String value:new String[]{"0.5", "2.0", "2147483648.0"})
   assertThrows(IllegalArgumentException.class,()->parse(model("0..1",value)));
  BoundedInteger range=new BoundedInteger(0,1);
  assertEquals(1,range.interpret("1.0").intValue());
  for(String value:new String[]{"0.5", "2.0", "2147483648.0"})
   assertThrows(recipe.lang.utils.exceptions.MismatchingTypeException.class,()->range.interpret(value));
 }
}
