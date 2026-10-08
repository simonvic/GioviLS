package it.simonvic.giovi.lsp;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;

public class Snippets {

	public static final List<CompletionItem> snippets = List.of(

		snippet(
			"hello_world",
			"This snippet is super handy!!!",
			"""
			"Hello world!" -> _out
			"""
		),

		snippet(
			"#",
			"Comment block",
			"""
			#{
			# $0
			}#
			"""
		),

		snippet(
			"#?",
			"Documentation block",
			"""
			#?{
			#? $0
			}#
			"""
		),

		snippet(
			"println",
			"Print to stdout",
			"""
			$1 -> _out
			"""
		),

		snippet(
			"print",
			"Append to stdout",
			"""
			$1 ->+ _out
			"""
		),

		snippet(
			"struct",
			"struct",
			"""
			${1:Typename} <
				$0
			>
			"""
		),

		snippet(
			"if",
			"if statement",
			"""
			if ${1:condition} do {
				$0
			}
			"""
		),

		snippet(
			"ifelse",
			"if else statement",
			"""
			if ${1:condition} {
				$2
			} else {
				$3
			}
			"""
		),

		snippet(
			"iff",
			"multi if statement",
			"""
			if ${1:subject} {
				$2 do {
					$0
				}
			}
			"""
		),

		snippet(
			"repeat",
			"repeat statement",
			"""
			repeat {
				$0
			}
			"""
		),

		snippet(
			"while",
			"repeat statement with condition",
			"""
			repeat if ${1:condition} {
				$0
			}
			"""
		),

		snippet(
			"fori",
			"for statement over range",
			"""
			for ${1:i} in ${2:0}..${3:n} do {
				$0
			}
			"""
		),

		snippet(
			"fore",
			"foreach statement",
			"""
			for ${1:iterator} in ${2:iterated} do {
				$0
			}
			"""
		),

		snippet(
			"foremap",
			"foreach key value statement",
			"""
			for ${1:key} : ${2:value} in ${3:map} do {
				$0
			}
			"""
		)
	);

	public static CompletionItem snippet(String label, String detail, String insertText) {
		var item = new CompletionItem();
		item.setLabel(label);
		item.setDetail(detail);
		item.setKind(CompletionItemKind.Snippet);
		item.setInsertTextFormat(InsertTextFormat.Snippet);
		item.setInsertText(insertText);
		return item;
	}


}
