package au.radsoft.ansidraw;

import au.radsoft.console.Color;

class Options
{
    interface Watch
    {
        void changedOptions();
    }
    
    char getChar()
    {
        return c_;
    }
    
    void setChar(char c)
    {
        c_ = c;
        changed();
    }
    
    void changed()
    {
        for (Watch w : watchers_)
            w.changedOptions();
    }
    
    private char c_ = (char) 1;
    
    Color fg_ = Color.WHITE;
    Color bg_ = Color.BLACK;
    
    Tool tool_ = new Tool.Pen();
    
    java.util.Vector<Watch> watchers_ = new java.util.Vector<Watch>();
}
