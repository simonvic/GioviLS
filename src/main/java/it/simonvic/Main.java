package it.simonvic;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.apache.commons.cli.*;

import it.simonvic.giovi.lsp.GioviLS;
import it.simonvic.giovi.lsp.proxy.GioviLSProxy;

public class Main {

	private static final Option OPT_HELP = Option.builder()
		.option("h")
		.longOpt("help")
		.desc("Show this help")
		.get();

	private static final Option OPT_VERBOSE = Option.builder()
		.option("v")
		.longOpt("verbose")
		.desc("")
		.get();


	private static final Option OPT_PROXY = Option.builder()
		.longOpt("proxy")
		.desc("Launch Giovi language server proxy")
		.get();

	private static final Options OPTIONS = optionsOf(
		OPT_HELP,
		OPT_VERBOSE,
		OPT_PROXY
	);

	private static Options optionsOf(Option... options) {
		Options opts = new Options();
		for (Option option : options) {
			opts.addOption(option);
		}
		return opts;
	}

	private static void printHelp() {
		HelpFormatter f = new HelpFormatter();
		f.setWidth(80);
		f.setOptionComparator(null);
		f.printHelp("Giovi", OPTIONS, true);
	}

	public static void main(String[] args)
		throws IOException, InterruptedException, ExecutionException, ParseException {
		CommandLine cli = new DefaultParser().parse(OPTIONS, args);

		long verbosity = Stream.of(cli.getOptions())
			.filter(OPT_VERBOSE::equals)
			.count();

		if (cli.hasOption(OPT_HELP)) {
			printHelp();
			return;
		}

		if (cli.hasOption(OPT_PROXY)) {
			new GioviLSProxy().run();
			return;
		}
		GioviLS.main(new String[] {});

	}

}
