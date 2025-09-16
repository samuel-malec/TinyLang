# TinyLang Specification

TinyLang is a minimal, educational programming language designed for learning and writing interpreters. 
It supports variables, arithmetic, control flow, functions, classes, and inheritance.

---
## Features
* Variables and assignment
* Arithmetic and boolean expressions
* Control flow: `if`, `else`, `while`, `for`
* Functions and first-class functions
* Classes and single inheritance

## Lexical Structure

### Identifiers

* Consist of letters (`a-z`, `A-Z`), digits (`0-9`), and underscores (`_`)
* Must not start with a digit

### Literals

* **Number literals**: sequences of digits, optionally with a decimal part (e.g., `42`, `123.45`)
* **String literals**: characters enclosed in double quotes (e.g., `"hello"`)
* **Boolean literals**: `true`, `false`
* **Nil literal**: `nil`

### Keywords

```
let fn return if else for while true false class print super this and or nil
```

### Operators

```
+  -  *  /  ==  !=  <  <=  >  >=  &&  ||  =
```

### Comments

* Single line: `// comment`

---

## Grammar

(EBNF-style)

```
program       ::= declaration* EOF

declaration   ::= classDecl 
                | funDecl 
                | varDecl 
                | statement

classDecl     ::= class IDENT ( "extends" IDENT )? "{" classMember* "}"

classMember   ::= classFunction | function

classFunction ::= "class" function

funDecl       ::= "fn" function 

varDecl       ::= "let" IDENT ("=" expression)? ";"

statement     ::= exprStmt 
                | forStmt 
                | ifStmt 
                | printStmt 
                | returnStmt 
                | whileStmt 
                | block

exprStmt      ::= expression ";" 

forStmt       ::= "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement 

ifStmt        ::= "if" "(" expression ")" statement ( "else" statement )? 

printStmt     ::= "print" expression ";" 

returnStmt    ::= "return" expression? ";" 

whileStmt     ::= "while" "(" expression ")" statement 

block         ::= "{" declaration* "}"

expression    ::= assignment 

assignment    ::= ( call "." )? IDENT "=" assignment 
                | logic_or 

logic_or      ::= logic_and ( "or" logic_and )* 

logic_and     ::= equality ( "and" equality )* 

equality      ::= comparison ( ( "!=" | "==" ) comparison )* 

comparison    ::= term ( ( ">" | ">=" | "<" | "<=" ) term )*
 
term          ::= factor ( ( "-" | "+" ) factor )* 
 
factor        ::= power ( ( "/" | "*" | "%" ) power )*

power         ::= unary ( "**" unary )*

unary         ::= ( "!" | "-" ) unary | call 

call          ::= primary ( "(" arguments? ")" | "." IDENT )* 

primary       ::= "true" 
                | "false" 
                | "nil" 
                | "this" 
                | NUMBER 
                | STRING 
                | IDENT 
                | "(" expression ")" 
                | "super" "." IDENT
                | "fn" "(" parameters? ")" block

function      ::= IDENT "(" parameters? ")" block 

parameters    ::= IDENT ( "," IDENT )* 

arguments     ::= expression ( "," expression )*

```

---

## Semantics

### Variables

* Declared with `let`
* Example:

  ```
  let x = 10;
  let y = x + 5;
  ```

### Functions

* Declared with `fn`
* Functions are **first-class values**
* Example:

  ```
  let add = fn(a, b) {
    return a + b;
  };
  let result = add(3, 4);
  ```

### Control Flow

* **If/Else**:

  ```
  if (x < 10) {
    x = x + 1;
  } else {
    x = 0;
  }
  ```

* **While**:

  ```
  while (x > 0) {
    x = x - 1;
  }
  ```
  
## Expressions

* Support standard arithmetic and boolean operators
* Example:

  ```
  let flag = (x > 0) and (y < 10);
  ```

## Classes and Inheritance
* Declared with `class`
* Support single inheritance with `extends`
* Example:
```
    class Animal {
         speak() {
            print "Animal sound";
        }
    }
    class Dog extends Animal {
        speak() {
            print "Woof!";
        }
    }
```  
## Lambda and Closures
* Anonymous functions can be created using `fn`
* Support closures, capturing variables from the surrounding scope
* Example:
```
let makeCounter = fn() {
  let count = 0;
  return fn() {
    count = count + 1;
    return count;
  };
};
let counter = makeCounter();
print counter(); // 1
print counter(); // 2
```


---

## Example Program

```
class Person {
    init(name, age) {
        this.name = name;
        this.age = age;
    }

    greet() {
        print "Hello, my name is " + this.name + " and I am " + this.age + " years old.";
    }
}

class Employee extends Person {
    init(name, age, position) {
        super.init(name, age);
        this.position = position;
    }

    work() {
        print "I am working as a " + this.position + ".";
    }
}

let john = Person("John", 30);
john.greet();
```

---