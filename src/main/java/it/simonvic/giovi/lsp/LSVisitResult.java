package it.simonvic.giovi.lsp;

import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeBool;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeInt;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeNatural;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeNumber.TypeReal;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeText;
import it.simonvic.giovi.lsp.LSVisitResult.Type.TypePrimitive.TypeVoid;

public sealed interface LSVisitResult {

	public static record Nothing() implements LSVisitResult {
	}

	public static record Default() implements LSVisitResult {
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

		public default boolean isNarrower(Type other) {
			return this.equals(other);
		}

		public static record TypeAny() implements Type {
		}

		public static record TypeSelf() implements Type {
		}

		public static record TypeType() implements Type {
		}

		public static record TypeFunction(FunctionSignature signauture) implements Type {

			public static record FunctionSignature(
				String identifier,
				Type returnType,
				Type... paramsTypes
			) {
			}

		}

		public static record TypeModule(Environment module) implements Type {
		}

		public static record TypeRange(Type type) implements Type {

			public static final TypeRange VOID = new TypeRange(Type.VOID);
			public static final TypeRange TEXT = new TypeRange(Type.TEXT);
			public static final TypeRange NAT = new TypeRange(Type.NAT);
			public static final TypeRange INT = new TypeRange(Type.INT);
			public static final TypeRange REAL = new TypeRange(Type.REAL);

		}

		public static sealed interface TypePrimitive extends Type {

			public static record TypeVoid() implements TypePrimitive {

				@Override
				public boolean isNarrower(Type other) {
					return true;
				}

			}

			public static record TypeBool() implements TypePrimitive {
			}

			public static record TypeText() implements TypePrimitive {
			}

			public static sealed interface TypeNumber extends TypePrimitive {

				public static record TypeNatural() implements TypeNumber {

					@Override
					public boolean isNarrower(Type other) {
						return switch (other) {
							case TypeNatural _,TypeInt _,TypeReal _ -> true;
							default -> false;
						};
					}

				}

				public static record TypeInt() implements TypeNumber {

					@Override
					public boolean isNarrower(Type other) {
						return switch (other) {
							case TypeInt _,TypeReal _ -> true;
							default -> false;
						};
					}


				}

				public static record TypeReal() implements TypeNumber {
				}

			}

		}

		public static sealed interface TypeCollection extends Type {

			public static record TypeList(Type val) implements TypeCollection {

				@Override
				public boolean isNarrower(Type other) {
					return switch (other) {
						case TypeList o -> this.val.isNarrower(o.val);
						default -> false;
					};
				}

			}

			public static record TypeMap(Type key, Type val) implements TypeCollection {

				@Override
				public boolean isNarrower(Type other) {
					return switch (other) {
						case TypeMap o -> this.key.isNarrower(o.key) && this.val.isNarrower(o.val);
						default -> false;
					};
				}

			}

		}

		public static sealed interface TypeProduct extends Type {

			public static record TypeTuple() implements TypeProduct {
			}

			public static record TypeRecord() implements TypeProduct {
			}

			public static record TypeStruct() implements TypeProduct {
			}

		}

	}

}

