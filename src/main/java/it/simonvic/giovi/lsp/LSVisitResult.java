package it.simonvic.giovi.lsp;

import it.simonvic.giovi.lsp.LSVisitResult.Type.TypeCollection.TypeList;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeBool;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeInt;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeNatural;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeReal;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeText;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeVoid;

public sealed interface LSVisitResult {

	public static final Nothing NOTHING = new Nothing();
	public static final Default DEFAULT = new Default();


	//@formatter:off
	default LSVisitResult positive() { return NOTHING; }
	default LSVisitResult negative() { return NOTHING; }
	default LSVisitResult plus(LSVisitResult rhs) { return NOTHING; }
	default LSVisitResult minus(LSVisitResult rhs) { return NOTHING; }
	default LSVisitResult multiply(LSVisitResult rhs) { return NOTHING; }
	default LSVisitResult divide(LSVisitResult rhs) { return NOTHING; }
	default LSVisitResult modulo(LSVisitResult rhs) { return NOTHING; }
	default LSVisitResult power(LSVisitResult rhs) { return NOTHING; }
	default LSVisitResult dot(LSVisitResult rhs) { return NOTHING; }
	//@formatter:on

	public static record Nothing() implements LSVisitResult {
	}

	public static record Default() implements LSVisitResult {
	}

	public static record ReservedIdentifier(String identifier) implements LSVisitResult {

		public static final ReservedIdentifier _OUT = new ReservedIdentifier("_out");
		public static final ReservedIdentifier _IN = new ReservedIdentifier("_in");
		public static final ReservedIdentifier _COUNT = new ReservedIdentifier("_count");
		public static final ReservedIdentifier _RANGE = new ReservedIdentifier("_range");
		public static final ReservedIdentifier _CHARS = new ReservedIdentifier("_chars");
		public static final ReservedIdentifier _KEYS = new ReservedIdentifier("_keys");
		public static final ReservedIdentifier _VALUES = new ReservedIdentifier("_values");

	}


	public static sealed interface Type extends LSVisitResult {

		public static final TypeAny ANY = new TypeAny();
		public static final TypeSelf SELF = new TypeSelf();
		public static final TypeVoid VOID = new TypeVoid();
		public static final TypeBool BOOL = new TypeBool();
		public static final TypeText TEXT = new TypeText();
		public static final TypeNatural NAT = new TypeNatural();
		public static final TypeInt INT = new TypeInt();
		public static final TypeReal REAL = new TypeReal();

		public static Type widestOf(Type... types) throws Exception {
			if (types.length == 0) return Type.VOID;
			Type inferred = null;
			Type next;
			for (var type : types) {
				next = type;
				if (inferred == null) {
					inferred = next;
					continue;
				}
				if (next.equals(inferred) || next.isNarrower(inferred)) {
					continue;
				}
				if (inferred.isNarrower(next)) {
					inferred = next;
					continue;
				}
				// find minimum widest type here or rework widening?
				throw new Exception(
					"Impossible to infer widest type.\n\tExpecting:\t" + inferred
						+ "\n\tbut got:\t" + next
				);
			}
			if (inferred == null) {
				throw new Exception("Impossible to infer widest type");
			}
			return inferred;
		}

		public default boolean isNarrower(Type other) {
			return this.equals(other);
		}

		public static record TypeAny() implements Type {

			@Override
			public String toString() {
				return "Any";
			}

		}

		public static record TypeSelf() implements Type {

			@Override
			public String toString() {
				return "Self";
			}

		}

		public static record TypeType(Type describedType) implements Type {

			@Override
			public String toString() {
				return "Type{" + describedType + "}";
			}

		}

		public static record TypeFunction(
			String identifier,
			Type returnType,
			Param... parameters
		) implements Type {

			public static final TypeFunction EMPTY = new TypeFunction("", Type.VOID);

			public static record Param(String label, String identifier, Type type) {

				public Param(String identifier, Type type) {
					this(identifier, identifier, type);
				}

				@Override
				public String toString() {
					return (label.equals(identifier))
						? identifier + " " + type
						: label + ":" + identifier + " " + type;
				}

			}

			@Override
			public String toString() {
				var sb = new StringBuilder(identifier);
				sb.append('(');
				for (int i = 0; i < parameters.length; i++) {
					sb.append(parameters[i]);
					sb.append(",");
				}
				sb.append(')');
				sb.append("=>");
				sb.append(returnType);
				return sb.toString();
			}

		}

		public static record TypeModule(Environment module) implements Type {

			@Override
			public String toString() {
				return "Module";
			}

		}

		public static record TypeRange(Type type) implements Type {

			public static final TypeRange VOID = new TypeRange(Type.VOID);
			public static final TypeRange TEXT = new TypeRange(Type.TEXT);
			public static final TypeRange NAT = new TypeRange(Type.NAT);
			public static final TypeRange INT = new TypeRange(Type.INT);
			public static final TypeRange REAL = new TypeRange(Type.REAL);

			@Override
			public String toString() {
				return "Range{" + type + "}";
			}

		}

		public static sealed interface TypePrimitive extends Type {

			public static record TypeVoid() implements TypePrimitive {

				@Override
				public String toString() {
					return "Void";
				}

				@Override
				public boolean isNarrower(Type other) {
					return true;
				}

			}

			public static record TypeBool() implements TypePrimitive {

				@Override
				public String toString() {
					return "Bool";
				}

			}

			public static record TypeText() implements TypePrimitive {

				@Override
				public String toString() {
					return "Text";
				}

				@Override
				public LSVisitResult plus(LSVisitResult rhs) {
					return switch (rhs) {
						case Type _ -> Type.TEXT;
						default -> NOTHING;
					};
				}

				@Override
				public LSVisitResult minus(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeText _ -> Type.TEXT;
						default -> NOTHING;
					};
				}

				@Override
				public LSVisitResult multiply(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeNumber _ -> Type.TEXT;
						default -> NOTHING;
					};
				}

				@Override
				public LSVisitResult divide(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeText _ -> TypeList.TEXT;
						default -> NOTHING;
					};
				}

				@Override
				public LSVisitResult power(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeNumber _ -> Type.TEXT;
						default -> NOTHING;
					};
				}

				@Override
				public LSVisitResult dot(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeNatural _,TypeInt _ -> Type.TEXT;
						case TypeRange(TypeNatural _),TypeRange(TypeInt _) -> TypeList.TEXT;
						case ReservedIdentifier(var i) -> switch (i) {
							case "_count" -> Type.INT;
							case "_range" -> TypeRange.INT;
							case "_chars" -> TypeList.TEXT;
							default -> NOTHING;
						};
						default -> NOTHING;
					};
				}

			}

			public static sealed interface TypeNumber extends TypePrimitive {

				@Override
				default TypeNumber positive() {
					return this;
				}

				@Override
				default TypeNumber negative() {
					return this;
				}

				@Override
				default LSVisitResult divide(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeNumber _ -> Type.REAL;
						default -> NOTHING;
					};
				}


				public static record TypeNatural() implements TypeNumber {

					@Override
					public String toString() {
						return "Natural";
					}

					@Override
					public boolean isNarrower(Type other) {
						return switch (other) {
							case TypeNatural _,TypeInt _,TypeReal _ -> true;
							default -> false;
						};
					}

					@Override
					public TypeNumber negative() {
						return Type.INT;
					}

					@Override
					public LSVisitResult plus(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _ -> Type.NAT;
							case TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult minus(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _,TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult multiply(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _ -> Type.NAT;
							case TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult power(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _ -> Type.NAT;
							case TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult modulo(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _ -> Type.NAT;
							case TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

				}

				public static record TypeInt() implements TypeNumber {

					@Override
					public String toString() {
						return "Int";
					}

					@Override
					public boolean isNarrower(Type other) {
						return switch (other) {
							case TypeInt _,TypeReal _ -> true;
							default -> false;
						};
					}

					@Override
					public LSVisitResult plus(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _,TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult minus(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _,TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult multiply(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _,TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult power(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _,TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult modulo(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNatural _,TypeInt _ -> Type.INT;
							case TypeReal _ -> Type.REAL;
							default -> NOTHING;
						};
					}

				}

				public static record TypeReal() implements TypeNumber {

					@Override
					public String toString() {
						return "Real";
					}

					@Override
					public LSVisitResult plus(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNumber _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult minus(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNumber _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult multiply(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNumber _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult power(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNumber _ -> Type.REAL;
							default -> NOTHING;
						};
					}

					@Override
					public LSVisitResult modulo(LSVisitResult rhs) {
						return switch (rhs) {
							case TypeNumber _ -> Type.REAL;
							default -> NOTHING;
						};
					}

				}

			}

		}

		public static sealed interface TypeCollection extends Type {

			public static record TypeList(Type val) implements TypeCollection {

				public static final TypeList VOID = new TypeList(Type.VOID);
				public static final TypeList TEXT = new TypeList(Type.TEXT);

				@Override
				public String toString() {
					return "[" + val + "]";
				}

				@Override
				public boolean isNarrower(Type other) {
					return switch (other) {
						case TypeList o -> this.val.isNarrower(o.val);
						default -> false;
					};
				}

				@Override
				public LSVisitResult plus(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeList(var val) when val.isNarrower(this.val) -> this;
						case Type val when val.isNarrower(this.val) -> this;
						default -> NOTHING;
					};
				}

				@Override
				public LSVisitResult minus(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeList(var val) when val.isNarrower(this.val) -> this;
						case Type val when val.isNarrower(this.val) -> this;
						default -> NOTHING;
					};
				}

				@Override
				public LSVisitResult dot(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeNatural _,TypeInt _ -> this.val;
						case TypeRange(TypeNatural _),TypeRange(TypeInt _) -> this;
						case ReservedIdentifier(var i) -> switch (i) {
							case "_count" -> Type.INT;
							case "_range" -> TypeRange.INT;
							default -> NOTHING;
						};
						default -> NOTHING;
					};
				}

			}

			public static record TypeMap(Type key, Type val) implements TypeCollection {

				public static final TypeMap VOID_VOID = new TypeMap(Type.VOID, Type.VOID);

				@Override
				public String toString() {
					return "[" + key + "=>" + val + "]";
				}

				@Override
				public boolean isNarrower(Type other) {
					return switch (other) {
						case TypeMap o -> this.key.isNarrower(o.key) && this.val.isNarrower(o.val);
						default -> false;
					};
				}

				@Override
				public LSVisitResult plus(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeMap(var key, var val) when key.isNarrower(this.key)
							&& val.isNarrower(this.val)
							-> this;
						default -> NOTHING;
					};
				}


				@Override
				public LSVisitResult minus(LSVisitResult rhs) {
					return switch (rhs) {
						case TypeMap(var key, var val) when key.isNarrower(this.key)
							&& val.isNarrower(this.val)
							-> this;
						default -> NOTHING;
					};
				}

				@Override
				public LSVisitResult dot(LSVisitResult rhs) {
					return switch (rhs) {
						case Type t when t.equals(this.key) -> this.val;
						case TypeRange(TypeNatural _),TypeRange(TypeInt _) -> this;
						case ReservedIdentifier(var i) -> switch (i) {
							case "_count" -> Type.INT;
							case "_keys" -> new TypeList(this.key);
							case "_values" -> new TypeList(this.val);
							default -> NOTHING;
						};
						default -> NOTHING;
					};
				}

			}

		}

		public static sealed interface TypeProduct extends Type {

			public static record TypeTuple(Type... fields) implements TypeProduct {

				@Override
				public String toString() {
					if (fields.length == 0) return "<>";
					var sb = new StringBuilder("<");
					for (int i = 0; i < fields.length; i++) {
						sb.append(fields[i]);
						sb.append(",");
					}
					sb.append(">");
					return sb.toString();
				}

			}

			public static record RecordField(String identifier, Type type) {

				@Override
				public String toString() {
					return identifier + " " + type;
				}

			}

			public static record TypeRecord(RecordField... fields) implements TypeProduct {

				@Override
				public String toString() {
					if (fields.length == 0) return "<>";
					var sb = new StringBuilder("<");
					for (int i = 0; i < fields.length; i++) {
						sb.append(fields[i]);
						sb.append(",");
					}
					sb.append(">");
					return sb.toString();
				}

			}

			public static record TypeStruct(
				String typename,
				RecordField[] fields,
				RecordField[] constants,
				TypeFunction[] functions
			) implements TypeProduct {

				public TypeStruct(String identifier) {
					this(
						identifier,
						new RecordField[] {},
						new RecordField[] {},
						new TypeFunction[] {}
					);
				}

				public TypeStruct(String identifier, RecordField... fields) {
					this(
						identifier,
						fields,
						new RecordField[] {},
						new TypeFunction[] {}
					);
				}

				@Override
				public String toString() {
					if (fields.length == 0) return typename + "<>";
					var sb = new StringBuilder(typename + "<");
					for (int i = 0; i < fields.length; i++) {
						sb.append(fields[i]);
						sb.append(",");
					}
					sb.append(">");
					return sb.toString();
				}

			}

		}

	}

}

