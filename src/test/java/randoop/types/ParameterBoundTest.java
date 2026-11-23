package randoop.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** Tests the construction of type parameters and their bounds. */
public class ParameterBoundTest {

  /**
   * A type parameter bounded by its declaring class applied to a wildcard (issue #1889).
   *
   * @param <T> the type parameter
   */
  interface WildcardSelfBound<T extends WildcardSelfBound<?>> {}

  /** An implementation of {@link WildcardSelfBound} that instantiates it with itself. */
  static class ConcreteWildcardSelfBound implements WildcardSelfBound<ConcreteWildcardSelfBound> {}

  /**
   * A type parameter whose bound refers to {@link MutualB}, which refers back to this class.
   *
   * @param <T> the type parameter
   */
  interface MutualA<T extends MutualB<?>> {}

  /**
   * A type parameter whose bound refers to {@link MutualA}, which refers back to this class.
   *
   * @param <U> the type parameter
   */
  interface MutualB<U extends MutualA<?>> {}

  /**
   * A type parameter whose bound is a class nested in this class.
   *
   * @param <T> the type parameter
   */
  static class EnclosingBound<T extends EnclosingBound.Inner> {
    /** A class nested in {@link EnclosingBound}. */
    static class Inner {}
  }

  /**
   * A class whose type parameter has a bound with a wildcard bounded by the class itself.
   *
   * @param <T> the type parameter
   */
  static class SelfBound<T extends List<? extends SelfBound<?>>> {}

  @Test
  public void testWildcardSelfBound() {
    assertEquals(1, GenericClassType.forClass(WildcardSelfBound.class).getTypeParameters().size());
  }

  /**
   * Checking a type against the bound {@code WildcardSelfBound<?>} terminates. The capture
   * conversion of the bound has a capture variable whose upper bound is the bound itself.
   */
  @Test
  public void testWildcardSelfBoundIsUpperBound() {
    TypeVariable t = GenericClassType.forClass(WildcardSelfBound.class).getTypeParameters().get(0);
    ReferenceType concrete = ClassOrInterfaceType.forClass(ConcreteWildcardSelfBound.class);
    assertTrue(t.getUpperTypeBound().isUpperBound(concrete, new Substitution()));
    assertFalse(t.getUpperTypeBound().isUpperBound(JavaTypes.STRING_TYPE, new Substitution()));
  }

  @Test
  public void testSelfBound() {
    assertEquals(1, GenericClassType.forClass(SelfBound.class).getTypeParameters().size());
  }

  @Test
  public void testMutuallyRecursiveBounds() {
    assertEquals(1, GenericClassType.forClass(MutualA.class).getTypeParameters().size());
    assertEquals(1, GenericClassType.forClass(MutualB.class).getTypeParameters().size());
  }

  @Test
  public void testEnclosingBound() {
    assertEquals(1, GenericClassType.forClass(EnclosingBound.class).getTypeParameters().size());
  }
}
