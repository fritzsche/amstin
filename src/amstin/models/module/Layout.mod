module Layout: Template

def main() 
  layout("This is the title") {
     p echo "Another test";
  }
end

// title as param shadows markup title
// TODO make a lisp-2?
def layout(t)
  html {
    head title echo t;
    body {
      h1 echo t;
      echo "Dit is een test";
      yield;
    }
  } 
end