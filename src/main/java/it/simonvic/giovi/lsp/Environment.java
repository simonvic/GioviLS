package it.simonvic.giovi.lsp;

import java.util.*;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;

import it.simonvic.giovi.lsp.LSVisitResult.Type;

public sealed class Environment {

	public static final class EnvironmentFunction extends Environment {

		public EnvironmentFunction(Environment parent, Range range) {
			super(parent, range);
		}

		public EnvironmentFunction(Range range) {
			super(range);
		}

	}

	public static final class EnvironmentLoop extends Environment {

		public EnvironmentLoop(Environment parent, Range range) {
			super(parent, range);
		}

		public EnvironmentLoop(Range range) {
			super(range);
		}

	}

	public static record Declaration(
		SymbolKind kind,
		Type type,
		Range range
	) {
	}

	private final Environment parent;
	private final Range range;
	private final List<Environment> children = new ArrayList<>();

	public Environment(Environment parent, Range range) {
		this.parent = parent;
		this.range = range;
		parent.children.add(this);
	}

	public Environment(Range range) {
		this.parent = null;
		this.range = range;
	}

	public Environment parent() {
		return parent;
	}

	public Range range() {
		return range;
	}

	public List<Environment> children() {
		return children;
	}

	/**
	 * Query the smallest environment that contains the given position
	 *
	 * <pre><code>
	 *  -----------------------------
	 *  |                           |
	 *  |    ______                 |
	 *  |   |_____|                 |
	 *  |     ______________        |
	 *  |    |  ____  ____ |        |
	 *  |    | |___| |___| |        |
	 *  |    |_____________|        |
	 *  |                           |
	 *  -----------------------------
	 *  </code></pre>
	 *
	 * @param position
	 * @return
	 */
	public Optional<Environment> query(Position position) {
		// TODO: convert to non-recursive
		if (!LSPUtils.isInRange(position, this.range)) {
			return Optional.empty();
		}
		for (var child : this.children) {
			var optEnv = child.query(position);
			if (optEnv.isPresent()) {
				return optEnv.orElseThrow().query(position);
			}
		}
		return Optional.of(this);
	}

	public Map<String, Declaration> lookupAll() {
		var items = new HashMap<String, Declaration>();
		// we get values in reverse lookup order, so we get shadowing for free
		// TODO: no, that's stupid, just start from bottom and check if contained already
		if (parent != null) {
			items.putAll(parent.lookupAll());
		}
		items.putAll(modules);
		items.putAll(types);
		items.putAll(functions);
		items.putAll(constants);
		items.putAll(variables);
		return items;
	}

	public boolean declare(
		Map<String, Declaration> environment,
		String identifier,
		Declaration declaration
	) {
		if (environment.containsKey(identifier)) {
			return false;
		}
		environment.put(identifier, declaration);
		return true;
	}

	public boolean declare(
		String identifier,
		Declaration declaration
	) {
		var env = switch (declaration.kind()) {
			case Variable -> variables;
			case Constant -> constants;
			case Function -> functions;
			case Struct -> types;
			case Module -> modules;
			default -> null;
		};
		if (env == null) return false;
		if (env.containsKey(identifier)) {
			return false;
		}
		env.put(identifier, declaration);
		return true;
	}

	public Optional<Declaration> lookup(String identifier) {
		return Optional.ofNullable(variables.get(identifier))
			.or(() -> Optional.ofNullable(constants.get(identifier)))
			.or(() -> Optional.ofNullable(functions.get(identifier)))
			.or(() -> Optional.ofNullable(types.get(identifier)))
			.or(() -> Optional.ofNullable(modules.get(identifier)))
			.or(() -> parent == null ? Optional.empty() : parent.lookup(identifier));
	}

	//////////////////////////////////////////////////////////////////////////////////////////////// Variables

	private final Map<String, Declaration> variables = new HashMap<>();

	public boolean declareVariable(String identifier, Declaration declaration) {
		return declare(variables, identifier, declaration);
	}

	public boolean isDeclaredVariable(String identifier) {
		return variables.containsKey(identifier)
			|| (parent != null && parent.isDeclaredVariable(identifier));
	}

	public Optional<Declaration> getVariable(String identifier) {
		if (variables.containsKey(identifier)) {
			return Optional.of(variables.get(identifier));
		}
		if (parent == null) {
			return Optional.empty();
		}
		return parent.getVariable(identifier);
	}


	//////////////////////////////////////////////////////////////////////////////////////////////// Constants

	private final Map<String, Declaration> constants = new HashMap<>();

	public boolean declareConstant(String identifier, Declaration declaration) {
		return declare(constants, identifier, declaration);
	}

	public boolean isDeclaredConstant(String identifier) {
		return constants.containsKey(identifier)
			|| (parent != null && parent.isDeclaredConstant(identifier));
	}

	public Optional<Declaration> getConstant(String identifier) {
		if (constants.containsKey(identifier)) {
			return Optional.of(constants.get(identifier));
		}
		if (parent == null) {
			return Optional.empty();
		}
		return parent.getConstant(identifier);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////// Functions

	private final Map<String, Declaration> functions = new HashMap<>();

	public boolean declareFunction(String identifier, Declaration declaration) {
		return declare(functions, identifier, declaration);
	}

	public boolean isDeclaredFunction(String identifier) {
		return functions.containsKey(identifier)
			|| (parent != null && parent.isDeclaredFunction(identifier));
	}

	public Optional<Declaration> getFunction(String identifier) {
		if (functions.containsKey(identifier)) {
			return Optional.of(functions.get(identifier));
		}
		if (parent == null) {
			return Optional.empty();
		}
		return parent.getFunction(identifier);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////// Declarations

	private final Map<String, Declaration> types = new HashMap<>();

	public static final Map<String, Declaration> primitiveTypes = Map.of(
		"Natural", new Declaration(SymbolKind.Class, Type.NAT, null),
		"Int", new Declaration(SymbolKind.Class, Type.INT, null),
		"Real", new Declaration(SymbolKind.Class, Type.REAL, null),
		"Bool", new Declaration(SymbolKind.Class, Type.BOOL, null),
		"Text", new Declaration(SymbolKind.Class, Type.TEXT, null)
	);

	public boolean declareType(String identifier, Declaration declaration) {
		return declare(types, identifier, declaration);
	}

	public boolean isDeclaredType(String identifier) {
		return types.containsKey(identifier)
			|| (parent != null && parent.isDeclaredType(identifier));
	}

	public Optional<Declaration> getType(String identifier) {
		return Optional.ofNullable(primitiveTypes.get(identifier))
			.or(() -> Optional.ofNullable(types.get(identifier)))
			.or(() -> (parent == null) ? Optional.empty() : parent.getType(identifier));
	}

	//////////////////////////////////////////////////////////////////////////////////////////////// Modules

	private final Map<String, Declaration> modules = new HashMap<>();

	public void declareModule(String identifier, Declaration declaration) {
		declare(modules, identifier, declaration);
	}

	public boolean isDeclaredModule(String identifier) {
		return modules.containsKey(identifier)
			|| (parent != null && parent.isDeclaredModule(identifier));
	}

	public Optional<Declaration> getModule(String identifier) {
		if (modules.containsKey(identifier)) {
			return Optional.of(modules.get(identifier));
		}
		if (parent == null) {
			return Optional.empty();
		}
		return parent.getModule(identifier);
	}

}
