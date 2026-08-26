package com.botmaker.plugin.api.catalog;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The identity of one member, recovered from the method reference that named it.
 *
 * <p>A {@link MemberRef} is {@code Serializable}, so javac gives its implementation class a synthetic
 * {@code writeReplace()} returning a {@link SerializedLambda}. Reflecting that one method yields the three
 * things an identity needs — the declaring class, the member name, and the JVM descriptor — with no parsing
 * of source and no string anywhere in the catalog's own text.
 *
 * <p>The descriptor is what makes an overload set tractable: {@code click(Point)} and {@code click(Rect)}
 * differ in it, so {@code Mouse::click} named twice with two type witnesses produces two distinct ids.
 *
 * <p><b>Constructors</b> report {@link #name()} as {@code <init>}; {@link #isConstructor()} says so.
 */
public record MemberId(Class<?> declaringClass, String name, String descriptor) {

    /** The name the JVM gives a constructor. */
    public static final String CONSTRUCTOR = "<init>";

    public MemberId {
        Objects.requireNonNull(declaringClass, "declaringClass");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(descriptor, "descriptor");
    }

    /**
     * Reads the identity out of a method reference.
     *
     * @throws IllegalArgumentException if {@code ref} is not a method reference or lambda compiled against
     *                                  one of the {@code M0..M5} shapes — a plain class implementing
     *                                  {@link MemberRef} by hand has no {@code writeReplace()} and names
     *                                  nothing
     */
    public static MemberId of(MemberRef ref) {
        Objects.requireNonNull(ref, "ref");
        SerializedLambda lambda = serialized(ref);
        String owner = lambda.getImplClass().replace('/', '.');
        Class<?> declaring;
        try {
            declaring = Class.forName(owner, false, ref.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            // Practically unreachable: the reference only compiled because the class was on the classpath,
            // and it is being read by the same loader. Reported rather than swallowed all the same.
            throw new IllegalArgumentException("catalog names a member of a class that cannot be loaded: " + owner, e);
        }
        return new MemberId(declaring, lambda.getImplMethodName(), lambda.getImplMethodSignature());
    }

    /**
     * The identity of a method already in hand — the route {@link CatalogBuilder#addAll()} takes, where there
     * is no method reference because nobody wrote one.
     *
     * <p>The descriptor is computed rather than read, and it must come out byte-identical to the one javac
     * puts in a {@link SerializedLambda}: the two routes into this record meet in
     * {@link PaletteCatalog#mergedWith}, in a Studio menu lookup and in every equality test below, and an id
     * that differs only in how it was built is an entry that silently matches nothing.
     */
    public static MemberId of(Method method) {
        Objects.requireNonNull(method, "method");
        return new MemberId(method.getDeclaringClass(), method.getName(),
                descriptor(method.getParameterTypes(), method.getReturnType()));
    }

    /** As {@link #of(Method)}, for a constructor; the name is {@link #CONSTRUCTOR} and the return type void. */
    public static MemberId of(Constructor<?> constructor) {
        Objects.requireNonNull(constructor, "constructor");
        return new MemberId(constructor.getDeclaringClass(), CONSTRUCTOR,
                descriptor(constructor.getParameterTypes(), void.class));
    }

    private static String descriptor(Class<?>[] parameterTypes, Class<?> returnType) {
        StringBuilder out = new StringBuilder("(");
        for (Class<?> parameterType : parameterTypes) {
            appendDescriptor(out, parameterType);
        }
        return appendDescriptor(out.append(')'), returnType).toString();
    }

    private static StringBuilder appendDescriptor(StringBuilder out, Class<?> type) {
        Class<?> element = type;
        while (element.isArray()) {
            out.append('[');
            element = element.getComponentType();
        }
        if (!element.isPrimitive()) {
            return out.append('L').append(element.getName().replace('.', '/')).append(';');
        }
        return out.append(switch (element.getName()) {
            case "boolean" -> 'Z';
            case "byte" -> 'B';
            case "char" -> 'C';
            case "short" -> 'S';
            case "int" -> 'I';
            case "long" -> 'J';
            case "float" -> 'F';
            case "double" -> 'D';
            default -> 'V';
        });
    }

    private static SerializedLambda serialized(MemberRef ref) {
        try {
            Method writeReplace = ref.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object replacement = writeReplace.invoke(ref);
            if (replacement instanceof SerializedLambda lambda) {
                return lambda;
            }
            throw new IllegalArgumentException("not a method reference: writeReplace returned " + replacement);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "a catalog entry must be a method reference (Mouse::click), not a hand-written "
                            + MemberRef.class.getSimpleName() + " implementation: " + ref.getClass().getName(), e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("could not read the method reference " + ref.getClass().getName(), e);
        }
    }

    public boolean isConstructor() {
        return CONSTRUCTOR.equals(name);
    }

    /** The fully-qualified name of the class declaring the member. */
    public String declaringClassName() {
        return declaringClass.getName();
    }

    /**
     * The declared parameter types, as source-style names ({@code com.botmaker.sdk.api.geometry.Point},
     * {@code int}, {@code java.lang.String[]}), in declaration order.
     *
     * <p>For an instance-method reference the receiver is <em>not</em> among them: the descriptor is the
     * method's own, so {@code ImageTemplate::width} reports no parameters even though its {@link M1} shape
     * has one type argument.
     */
    public List<String> parameterTypeNames() {
        List<String> names = new ArrayList<>();
        int i = descriptor.indexOf('(') + 1;
        int end = descriptor.indexOf(')');
        while (i < end) {
            int dimensions = 0;
            while (descriptor.charAt(i) == '[') {
                dimensions++;
                i++;
            }
            String name;
            char c = descriptor.charAt(i);
            if (c == 'L') {
                int semi = descriptor.indexOf(';', i);
                name = descriptor.substring(i + 1, semi).replace('/', '.');
                i = semi + 1;
            } else {
                name = primitive(c);
                i++;
            }
            names.add(name + "[]".repeat(dimensions));
        }
        return List.copyOf(names);
    }

    private String primitive(char c) {
        return switch (c) {
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            case 'D' -> "double";
            case 'V' -> "void";
            default -> throw new IllegalStateException("unreadable descriptor " + descriptor);
        };
    }

    /**
     * A human-readable signature — {@code click(com.botmaker.sdk.api.geometry.Point)} — the spelling Studio's
     * surface queries have always used to tell one overload from another.
     */
    public String signature() {
        return name + "(" + String.join(",", parameterTypeNames()) + ")";
    }

    @Override
    public String toString() {
        return declaringClass.getName() + "#" + signature();
    }
}
