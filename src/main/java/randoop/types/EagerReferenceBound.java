package randoop.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bound on a type variable where the bound is a {@link ReferenceType} that can be used
 * directly. Contrast with {@link LazyReferenceBound}.
 */
class EagerReferenceBound extends ReferenceBound {

  /**
   * Creates a bound for the given reference type.
   *
   * @param boundType the reference boundType
   */
  /*package*/ EagerReferenceBound(ReferenceType boundType) {
    super(boundType);
  }

  /**
   * The checks of {@link #isUpperBound(Type, Substitution)} for a parameterized bound type that are
   * in progress on this thread, each a pair of the bound type and the argument type.
   */
  private static final ThreadLocal<List<ReferenceType[]>> upperBoundChecks =
      ThreadLocal.withInitial(ArrayList::new);

  @Override
  public EagerReferenceBound substitute(Substitution substitution) {
    ReferenceType referenceType = getBoundType().substitute(substitution);
    if (Type.sameType(referenceType, getBoundType())) {
      return this;
    }
    return new EagerReferenceBound(referenceType);
  }

  @Override
  public List<TypeVariable> getTypeParameters() {
    return getBoundType().getTypeParameters();
  }

  @Override
  public boolean isLowerBound(Type argType, Substitution subst) {
    ReferenceType thisBoundType = this.getBoundType().substitute(subst);
    if (thisBoundType.equals(JavaTypes.NULL_TYPE)) {
      return true;
    }
    if (thisBoundType.isVariable()) {
      return thisBoundType.isSubtypeOfOrEqualTo(argType);
    }
    if (argType.isParameterized()) {
      if (!(thisBoundType instanceof ClassOrInterfaceType)) {
        return false;
      }
      InstantiatedType argClassType = (InstantiatedType) argType.applyCaptureConversion();
      InstantiatedType boundSuperType =
          ((ClassOrInterfaceType) thisBoundType)
              .getMatchingSupertype(argClassType.getGenericClassType());
      if (boundSuperType == null) {
        return false;
      }
      boundSuperType = boundSuperType.applyCaptureConversion();
      return boundSuperType.isInstantiationOf(argClassType);
    }
    return thisBoundType.isSubtypeOfOrEqualTo(argType);
  }

  @Override
  public boolean isSubtypeOfOrEqualTo(ParameterBound bound) {
    if (bound instanceof ReferenceBound) {
      return this.getBoundType().isSubtypeOfOrEqualTo(((ReferenceBound) bound).getBoundType());
    }
    // TODO: change this into an unconditional exception.
    assert false : "not handling EagerReferenceBound subtype of other bound type";
    return false;
  }

  @Override
  public boolean isUpperBound(Type argType, Substitution subst) {
    ReferenceType thisBoundType = this.getBoundType().substitute(subst);
    if (thisBoundType.equals(JavaTypes.OBJECT_TYPE)) {
      return true;
    }
    if (thisBoundType.isVariable()) {
      // The substitution does not map the variable, so the variable can still be chosen.  argType
      // is within this bound if it is within the variable's upper bound, which the variable can be.
      return ((TypeVariable) thisBoundType).getUpperTypeBound().isUpperBound(argType, subst);
    }
    if (thisBoundType.isParameterized()) {
      if (!(argType instanceof ClassOrInterfaceType)) {
        return false;
      }
      // The capture conversion of the bound can have a capture variable whose upper bound is the
      // bound itself:  for T extends Builder<?>, the capture conversion of the bound is
      // Builder<CAP>, where CAP extends Builder<?>.  Checking argType against that capture
      // variable checks argType against this bound again, with a new capture variable each time.
      // As for any recursive bound, assume that the inner check succeeds.
      ReferenceType[] check = {thisBoundType, (ReferenceType) argType};
      List<ReferenceType[]> inProgress = upperBoundChecks.get();
      for (ReferenceType[] other : inProgress) {
        if (other[0].equals(check[0]) && other[1].equals(check[1])) {
          return true;
        }
      }
      inProgress.add(check);
      try {
        InstantiatedType boundClassType = (InstantiatedType) thisBoundType.applyCaptureConversion();
        InstantiatedType argSuperType =
            ((ClassOrInterfaceType) argType)
                .getMatchingSupertype(boundClassType.getGenericClassType());
        if (argSuperType == null) {
          return false;
        }
        argSuperType = argSuperType.applyCaptureConversion();
        return argSuperType.isInstantiationOf(boundClassType);
      } finally {
        inProgress.remove(inProgress.size() - 1);
      }
    }
    return argType.isSubtypeOfOrEqualTo(thisBoundType);
  }
}
