lexer grammar GioviExtendedLexer;

import GioviLexer;

channels {
	COMMENTS,
	DOCS,
	WHITESPACES
}

DOC_BLOCK:     '#{?' .*? ('}#' | EOF) -> channel(DOCS) ;
COMMENT_BLOCK: '#{' (~[?] .*?)? ('}#' | EOF) -> channel(COMMENTS);
DOC:           '#?' ~[\r\n]* ([\r\n] | EOF) -> channel(DOCS) ;
COMMENT:       '#' (~[?{\r\n] ~[\r\n]*)? ([\r\n] | EOF) -> channel(COMMENTS);

SPACES: [ ]+ -> channel(WHITESPACES) ;
TABS: [\t]+ -> channel(WHITESPACES) ;
NEWLINES: [\r\n]+ -> channel(WHITESPACES) ;
