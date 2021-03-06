module Template: Grammar

  Definitions ::= definitions:Definition*

  Definition ::= "def" name:key formals:Formals body:Statement "end"
  
  Formals ::= "(" params:{Param ","}* ")"

// Problem here with unparse (and possibly elsewhere): list of id's is not supported
// so introduce extra indirection (Param)

  Param ::= name:id
  
  Statement ::= [Element] tag:Tag args:Args? body:Statement
             | [Block] "{" statements:Statement* "}"
             | [IfElse] "if" "(" cond:Expression ")" then:Statement "else" otherwise:Statement
             | [If] "if" "(" cond:Expression ")" then:Statement
             | [Each] "each" "(" var:id ":" iter:Expression ")" body:Statement
             | [Echo] "echo" expression:Expression ";"
             | [Comment] "comment" expression:Expression ";"
             | [CData] "cdata" expression:Expression ";"
             | [Yield] "yield" ";"
  
  Args ::= "(" args:{Arg ","}* ")"
  
  Tag ::=  name:id suffixes:Suffix*
  
  Suffix ::= [Class] "." name:id | [Id] "#" name:id 
  
  Arg ::= [Keyword] name:id "=" expression:Expression | [Actual] expression:Expression
  
  Expression ::= [Str] value:str 
  			  | [Int] value:int 
  			  | [Var] name:id 
  			  | [Field] expression:Expression "." field:id 

