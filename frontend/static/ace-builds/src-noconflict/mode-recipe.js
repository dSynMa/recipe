ace.define("ace/mode/recipe",["require","exports","module","ace/lib/oop","ace/mode/text","ace/mode/text_highlight_rules","ace/mode/behaviour"], function(require, exports, module) {
    "use strict";
    
    var oop = require("../lib/oop");
    var TextMode = require("./text").Mode;
    var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;
    var Behaviour = require("./behaviour").Behaviour;
      
    var RecipeHighlightRules = function() {
        var keywords = "channels|receive|rep|message|structure|communication|guard|relabel|agent|repeat|channel|chan|int|bool|real|local|spec|LTLSPEC|PSLSPEC|INVARIANT|SPEC";
    
        var builtinConstants = (
            "true|false|null"
        );
    
        var builtinFunctions = (
            ""
        );
    
        var keywordMapper = this.createKeywordMapper({
            "support.function": builtinFunctions,
            "keyword": keywords,
            "constant.language": builtinConstants
        }, "identifier", true);
    
        this.$rules = {
            "start" : [ {
                token : "comment",
                regex : "--.*$"
            }, {
                token : "string",           // " string
                regex : '".*?"'
            }, {
                token : "string",           // character 
                regex : "'.'"
            }, {
                token : "constant.numeric", // float
                regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b"
            }, {
                token : keywordMapper,
                regex : "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
            }, {
                token : "keyword.operator",
                regex : ":|\\+|\\-|\\/|\\/\\/|%|<@>|@>|<@|&|\\^|~|<|>|<=|=>|==|!=|<>|=|:=|\\?|\\!|\\*"
            }, {
                token : "paren.lparen",
                regex : "[\\(]"
            }, {
                token : "paren.rparen",
                regex : "[\\)]"
            }, {
                token : "text",
                regex : "\\s+"
            } ]
        };
    };
    
    oop.inherits(RecipeHighlightRules, TextHighlightRules);
    
    exports.RecipeHighlightRules = RecipeHighlightRules;
    
    var Mode = function() {
        this.HighlightRules = RecipeHighlightRules;
        this.$behaviour = new Behaviour();
    };
    
    oop.inherits(Mode, TextMode);
    
    (function() {
        this.type = "text";
        this.getNextLineIndent = function(state, line, tab) {
            return '';
        };
        this.$id = "ace/mode/recipe";
    }).call(Mode.prototype);
    
    exports.Mode = Mode;
    });                (function() {
                        ace.require(["ace/mode/recipe"], function(m) {
                            if (typeof module == "object" && typeof exports == "object" && module) {
                                module.exports = m;
                            }
                        });
                    })();
                