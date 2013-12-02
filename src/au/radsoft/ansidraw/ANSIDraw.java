package au.radsoft.ansidraw;

import au.radsoft.textui.*;

import au.radsoft.console.CharInfo;
import au.radsoft.console.CharKey;
import au.radsoft.console.Color;
import au.radsoft.console.Console;
import au.radsoft.console.ConsoleUtils;
import au.radsoft.console.Event;
import au.radsoft.console.Buffer;

// TODO
// http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Runtime.html#addShutdownHook%28java.lang.Thread%29
// Runtime.getRuntime().addShutdownHook(new Thread() {
//    public void run() { /*
//       /my shutdown code here
//    */ }
// });
//
// Proper use of clipboard
// Add edit menu
// Create toolbar for LayerSelect commands
// Image resize

class ANSIDraw extends Screen
{
    public static void main(String s[])
        throws java.io.IOException
    {
        //ConsoleUtils.realloc();
        Console console = ConsoleUtils.create("ANSI Draw", 80, 50, true);
        try
        {
            console.showCursor(false);
            console.enableMouse(true);
        
            ANSIDraw ad = new ANSIDraw(console);
            ad.setStatus("Ready");
            ad.doeventloop(console);
        }
        finally
        {
            console.clear();
            console.close();
        }
    }
    
    static <E extends Enum<E>> E next(E e, E[] v)
    {
        int o = e.ordinal();
        return o < (v.length - 1) ? v[o + 1] : v[0];
    }
    
    static <E extends Enum<E>> E prev(E e, E[] v)
    {
        int o = e.ordinal();
        return o > 0 ? v[o - 1] : v[v.length - 1];
    }
    
    static Color next(Color c)
    {
        return next(c, Color.values());
    }
    
    static Color prev(Color c)
    {
        return prev(c, Color.values());
    }
    
    MenuList GetMainMenu()
    {
        return new MenuList("Main",
            new MenuList("File",
                new MenuCommand.Default("New") {
                    @Override
                    public void DoCommand(ConsoleState state)
                    {
                        CommandNew(state.GetConsole());
                    }
                },
                
                new MenuCommand.Default("Load") {
                    @Override
                    public void DoCommand(ConsoleState state)
                    {
                        CommandLoad(state.GetConsole());
                    }
                },
                
                new MenuCommand.Default("Save") {
                    @Override
                    public void DoCommand(ConsoleState state)
                    {
                        CommandSave(state.GetConsole());
                    }
        
                    @Override
                    public boolean IsEnabled()
                    {
                        return CommandSaveEnabled();
                    }
                },
                
                new MenuCommand.Default("Exit") {
                    @Override
                    public void DoCommand(ConsoleState state)
                    {
                        if (PromptExit(state.GetConsole()))
                            state.exit();
                    }
                }
            ),
            new MenuList("Help",
                new MenuCommand.Default("About") {
                    @Override
                    public void DoCommand(ConsoleState state)
                    {
                        Package objPackage = getClass().getPackage();
                        MsgBox.domodal(state.GetConsole(), "About",
                            objPackage.getSpecificationTitle()
                            + " Version: " + objPackage.getSpecificationVersion(),
                            15);
                    }
                }
            )
        );
    }

    static class CharSelect extends ToolWindow implements Options.Watch
    {
        public CharSelect(int ox, int oy, Options o, StatusBar sb)
        {
            super(ox, oy, 16, 16);
            setTitle("Character");
            o_ = o;
            o_.watchers_.add(this);
            sb_ = sb;
        }
        
        @Override
        protected void draw(Buffer w, int wx, int wy)
        {
            //System.err.println("CharSelect::draw " + wx + " " + wy);
            super.draw(w, wx, wy);
            char oc = o_.getChar();
            int sx = oc % 16;
            int sy = oc / 16;
            for (int y = 0; y < 16; ++y)
            {
                for (int x = 0; x < 16; ++x)
                {
                    char c = (char) (y * 16 + x);
                    Color color = Color.BLACK;
                    if (x == sx && y == sy)
                        color = Color.WHITE;
                    else if (x == sx || y == sy)
                        color = Color.CYAN;
                    w.write(x + wx, y + wy, c, color);
                    //w.write(x + wx, y + wy, (char) 1);
                }
            }
        }
        
        @Override
        public void onEvent(ConsoleState state, Event.Key kev)
        {
            super.onEvent(state, kev);
            
            if (kev.state == Event.State.PRESSED)
            {
                char c = o_.getChar();
                switch (kev.key)
                {
                case W:
                case UP:
                    c -= 16;
                    System.out.println("c: " + (int) c);
                    if (c > 255)
                        c += 256;
                    o_.setChar(c);
                    break;
                    
                case D:
                case RIGHT:
                    c += 1;
                    if (c > 255)
                        c -= 256;
                    o_.setChar(c);
                    break;

                case S:
                case DOWN:
                    c += 16;
                    if (c > 255)
                        c -= 256;
                    o_.setChar(c);
                    break;
                    
                case A:
                case LEFT:
                    c -= 1;
                    if (c > 255)
                        c += 256;
                    o_.setChar(c);
                    break;
                }
            }
        }
        
        @Override
        public void onEvent(ConsoleState state, Event.MouseButton mbev)
        {
            super.onEvent(state, mbev);
            
            if (mbev.state == Event.State.PRESSED
                && mbev.key == CharKey.MOUSE_BUTTON1)
            {
                o_.setChar((char) ((mbev.my - oy_) * 16 + (mbev.mx - ox_)));
            }
        }
        
        @Override
        public void onEvent(ConsoleState state, Event.MouseMoved mmev)
        {
            super.onEvent(state, mmev);
            int sx = mmev.mx - ox_;
            int sy = mmev.my - oy_;
            sb_.set(String.format("Char: %d", (sy * 16 + sx)));
        }
        
        @Override
        public void changedOptions()
        {
            invalid_ = true;
        }
        
        private final Options o_;
        private final StatusBar sb_;
    }
    
    static class ColorSelect extends ToolWindow implements Options.Watch
    {
        public ColorSelect(int ox, int oy, Options o)
        {
            super(ox, oy, Color.values().length, 3);
            setTitle("Colour");
            o_ = o;
            o_.watchers_.add(this);
        }
        
        @Override
        protected void draw(Buffer w, int wx, int wy)
        {
            super.draw(w, wx, wy);
            Color[] colors = Color.values();
            int y = 0;
            for (int x = 0; x < colors.length; ++x)
            {
                Color color = colors[x];
                w.write(x + wx, y + wy + 0, color.equals(o_.fg_) ? (char) 25 : ' ');
                w.write(x + wx, y + wy + 1, (char) 219, color, Color.WHITE);
                w.write(x + wx, y + wy + 2, color.equals(o_.bg_) ? (char) 24 : ' ');
            }
        }
        
        @Override
        public void onEvent(ConsoleState state, Event.Key kev)
        {
            super.onEvent(state, kev);
            
            if (kev.state == Event.State.PRESSED)
            {
                switch (kev.key)
                {
                case F:
                    if (state.getKeyDown(CharKey.SHIFT))
                        o_.fg_ = prev(o_.fg_);
                    else
                        o_.fg_ = next(o_.fg_);
                    invalid_ = true;
                    break;
                    
                case B:
                    if (state.getKeyDown(CharKey.SHIFT))
                        o_.bg_ = prev(o_.bg_);
                    else
                        o_.bg_ = next(o_.bg_);
                    invalid_ = true;
                    break;
                }
            }
        }
            
        @Override
        public void onEvent(ConsoleState state, Event.MouseButton mbev)
        {
            if (mbev.state == Event.State.PRESSED
                && mbev.key == CharKey.MOUSE_BUTTON1)
            {
                switch (mbev.my - oy_)
                {
                case 0:
                    o_.fg_ = Color.values()[mbev.mx - ox_];
                    invalid_ = true;
                    break;
                    
                case 2:
                    o_.bg_ = Color.values()[mbev.mx - ox_];
                    invalid_ = true;
                    break;
                }
            }
        }
        
        @Override
        public void changedOptions()
        {
            invalid_ = true;
        }
        
        private Options o_;
    }
    
    
    static class ColorSelect2 extends ToolWindow implements Options.Watch
    {
        public ColorSelect2(int ox, int oy, Options o)
        {
            super(ox, oy, Color.values().length, 1);
            setTitle("Colour");
            o_ = o;
            o_.watchers_.add(this);
        }
        
        @Override
        protected void draw(Buffer w, int wx, int wy)
        {
            super.draw(w, wx, wy);
            Color[] colors = Color.values();
            int y = 0;
            for (int x = 0; x < colors.length; ++x)
            {
                Color color = colors[x];
                if (color.equals(o_.fg_))
                    w.write(x + wx, y + wy, 'F', x < (colors.length / 2) ? Color.WHITE : Color.BLACK, color);
                else if (color.equals(o_.bg_))
                    w.write(x + wx, y + wy, 'B', x < (colors.length / 2) ? Color.WHITE : Color.BLACK, color);
                else
                    w.write(x + wx, y + wy, (char) 219, color, Color.WHITE);
            }
        }
        
        @Override
        public void onEvent(ConsoleState state, Event.Key kev)
        {
            super.onEvent(state, kev);
            
            if (kev.state == Event.State.PRESSED)
            {
                switch (kev.key)
                {
                //case F:
                    //if (state.getKeyDown(CharKey.SHIFT))
                        //o_.fg_ = prev(o_.fg_);
                    //else
                        //o_.fg_ = next(o_.fg_);
                    //invalid_ = true;
                    //break;
                    
                //case B:
                    //if (state.getKeyDown(CharKey.SHIFT))
                        //o_.bg_ = prev(o_.bg_);
                    //else
                        //o_.bg_ = next(o_.bg_);
                    //invalid_ = true;
                    //break;
                }
            }
        }
        
        @Override
        public void onEvent(ConsoleState state, Event.MouseButton mbev)
        {
            super.onEvent(state, mbev);
            
            if (mbev.state == Event.State.PRESSED)
            {
                switch (mbev.key)
                {
                case MOUSE_BUTTON1:
                    o_.fg_ = Color.values()[mbev.mx - ox_];
                    invalid_ = true;
                    break;
                    
                case MOUSE_BUTTONR:
                    o_.bg_ = Color.values()[mbev.mx - ox_];
                    invalid_ = true;
                    break;
                }
            }
        }
        
        @Override
        public void changedOptions()
        {
            invalid_ = true;
        }
        
        private Options o_;
    }
    
    static class ToolSelect extends ToolWindow
    {
        static Class toolsunchecked_[] = { Tool.Pen.class, Tool.Fill.class, Tool.Line.class, Tool.Square.class, Tool.Erase.class, Tool.Pick.class, Tool.Select.class };
        @SuppressWarnings("unchecked")  // TODO Is there a way around this?
        static Class<? extends Tool> tools_[] = (Class<? extends Tool>[]) toolsunchecked_;
        
        public ToolSelect(int ox, int oy, Options o)
        {
            super(ox, oy, 16, tools_.length);
            setTitle("Tool");
            o_ = o;
        }
        
        @Override
        protected void draw(Buffer w, int wx, int wy)
        {
            super.draw(w, wx, wy);
            int x = 0;
            for (int y = 0; y < tools_.length; ++y)
            {
                Class<? extends Tool> tool = tools_[y];
                char prefix = o_.tool_.getClass() == tool ? (char) 7 : ' ';
                w.write(x + wx, y + wy, prefix + tool.getSimpleName());
            }
        }
        
        @Override
        public void onEvent(ConsoleState state, Event.MouseButton mbev)
        {
            super.onEvent(state, mbev);
            
            if (mbev.state == Event.State.PRESSED
                && mbev.key == CharKey.MOUSE_BUTTON1)
            {
                Class<? extends Tool> tool = tools_[mbev.my - oy_];
                try
                {
                    o_.tool_ = tool.newInstance();
                    invalid_ = true;
                } catch (InstantiationException e) {
                    // TODO Report error
                } catch (IllegalAccessException e) {
                    // TODO Report error
                }
            }
        }
        
        private Options o_;
    }
    
    static class LayerSelect extends ToolWindow implements Canvas.Watch
    {
        static String commands_ = "+-" + (char)(24) + (char)(25);
        
        public LayerSelect(int ox, int oy, Canvas c)
        {
            super(ox, oy, 16, 8);
            setTitle("Layer");
            c_ = c;
            c_.watchers_.add(this);
        }
        
        @Override
        protected void draw(Buffer w, int wx, int wy)
        {
            super.draw(w, wx, wy);
            w.fill(wx, wy, w_, h_, ' ');
            w.write(w_ - commands_.length() + wx, 0 + wy, commands_);
            {
                Layer selected = c_.GetActiveLayer();
                int x = 0;
                int y = 1;
                for (int i = scroll_; i < c_.GetLayers().size() && (i - scroll_) < h_; ++i)
                {
                    Layer l = c_.GetLayers().get(i);
                    String s;
                    // TODO Limit name to space
                    if (l.visible_)
                        s = (char) 43 + l.GetName();
                    else
                        s = (char) 45 + l.GetName();
                        
                    if (l == selected)
                        w.write(x + wx, y + wy, s, Color.BLACK, Color.WHITE);
                    else
                        w.write(x + wx, y + wy, s);
                    ++y;
                }
            }
            {   // Draw scrollbar
                int x = w_ + wx - 1;
                int y = 1 + wy;
                int h = h_ - 3;
                w.write(x, y, (char) 30, Color.BLACK, Color.WHITE);
                w.write(x, y + h + 1, (char) 31, Color.BLACK, Color.WHITE);
                for (int sy = 0; sy < h; ++sy)
                {
                    int hb = sy * c_.GetLayers().size() / h;
                    int he = (sy + 1) * c_.GetLayers().size() / h;
                    //System.err.println("Scrollbar: " + sy + " " + hb + " " + he + " " + scroll_ + " " + (h_ - 1));
                    if (scroll_ <= he && (scroll_ + h_ - 1) > hb)
                        w.write(x, sy + y + 1, (char) 219, Color.WHITE, Color.BLACK);
                    else
                        w.write(x, sy + y + 1, (char) 177, Color.WHITE, Color.BLACK);
                }
                //System.err.println();
            }
        }
        
        @Override
        public void onEvent(ConsoleState state, Event.MouseButton mbev)
        {
            super.onEvent(state, mbev);
            
            if (mbev.state == Event.State.PRESSED
                && mbev.key == CharKey.MOUSE_BUTTON1)
            {
                int ly = mbev.my - oy_;
                int lx = mbev.mx - ox_;
                //System.err.println("Layer ly " + ly + " " + scroll_ + " " + Math.min(h_, c_.GetLayers().size() - scroll_));
                if (ly == 0)
                {
                    switch (w_ - lx)
                    {
                    case 1: // Move down
                        c_.MoveLayerDown(c_.GetActiveLayer());
                        break;
                        
                    case 2: // Move up
                        c_.MoveLayerUp(c_.GetActiveLayer());
                        break;
                        
                    case 3: // Delete
                        if (c_.GetLayers().size() >= 2)
                            c_.DeleteLayer(c_.GetActiveLayer());
                        break;
                        
                    case 4: // Add
                        c_.AddLayer();
                        break;
                        
                    default:
                        System.err.println("Layer tool " + (w_ - lx));
                    }
                }
                else if (lx == (w_ - 1))
                {   // On scrollbar
                    //System.err.println("Scrollbar: " + ly);
                    if (ly == 1)
                    {
                        if (scroll_ > 0)
                        {
                            --scroll_;
                            invalid_ = true;
                        }
                    }
                    else if (ly == (h_ - 1))
                    {
                        if (scroll_ < (c_.GetLayers().size() - 1 - (h_ - 2)))
                        {
                            ++scroll_;
                            invalid_ = true;
                        }
                    }
                }
                else if (ly >= 1 && ly <= Math.min(h_, c_.GetLayers().size() - scroll_))
                {
                    Layer sel = c_.GetLayers().get(ly - 1 - scroll_);
                    if (lx == 0)
                    {
                        c_.SetLayerVisiblity(sel, !sel.visible_);
                        invalid_ = true;
                    }
                    else if (c_.GetActiveLayer() != sel)
                    {
                        c_.SetActiveLayer(sel);
                        invalid_ = true;
                    }
                }
            }
            else if (mbev.state == Event.State.RELEASED
                && mbev.key == CharKey.MOUSE_BUTTONR)
            {
                int ly = mbev.my - oy_;
                int lx = mbev.mx - ox_;
                //System.err.println("Layer ly " + ly + " " + scroll_ + " " + Math.min(h_, c_.GetLayers().size() - scroll_));
                if (ly >= 1 && ly <= Math.min(h_, c_.GetLayers().size() - scroll_))
                {
                    Layer sel = c_.GetLayers().get(ly - 1 - scroll_);
                    
                    QuestionBox qb = new QuestionBox("Layer", "Name:", sel.GetName(), 21);
                    qb.centre(state.GetConsole());
                    if (qb.domodal(state.GetConsole()) > 0)
                    {
                        sel.SetName(qb.getValue());
                        invalid_ = true;
                    }
                }
            }
        }
        
        @Override
        public void changedCanvas(Section s)
        {
            if (s == Section.LAYERS)
                invalid_ = true;
        }
        
        private int scroll_ = 0;
        private Canvas c_;
    }
    
    final Canvas canvas_;
    
    ANSIDraw(Console console)
    {
        super(console);
        
        setMenu(GetMainMenu());
        
        Options o = new Options();
        
        Window l = null;
        
        add(l = new CharSelect(1, 2, o, getStatusBar()));
        
        int consolex = l.ox_ + l.w_ + 2;
        canvas_ = new Canvas(consolex, l.oy_, w_ - consolex - 1, h_ - 4, o, getStatusBar());
        
        //add(l = new ColorSelect(1, l.oy_ + l.h_ + 2, o));
        add(l = new ColorSelect2(1, l.oy_ + l.h_ + 2, o));
        add(l = new ToolSelect(1, l.oy_ + l.h_ + 2, o));
        add(l = new LayerSelect(1, l.oy_ + l.h_ + 2, canvas_));
        add(canvas_);
    }
    
    void CommandNew(Console console)
    {
        Buffer aw = canvas_.GetActiveLayer().GetBuffer();
        NewDlg nd = new NewDlg(aw.getWidth(), aw.getHeight());
        nd.centre(console);
        if (nd.domodal(console) > 0)
        {
            canvas_.New(nd.getWidthValue(), nd.getHeightValue());
        }
    }
    
    void CommandLoad(Console console)
    {
        try
        {
            QuestionBox qb = new QuestionBox("Load", "File name:", canvas_.getFilename(), 21);
            qb.centre(console);
            if (qb.domodal(console) > 0)
            {
                canvas_.Load(qb.getValue());
            }
        }
        catch (ClassNotFoundException ex)
        {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
        catch (java.io.FileNotFoundException ex)
        {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
        catch (java.io.IOException ex)
        {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }
    
    void CommandSave(Console console)
    {
        try
        {
            QuestionBox qb = new QuestionBox("Save", "File name:", canvas_.getFilename(), 21);
            qb.centre(console);
            if (qb.domodal(console) > 0)
            {
                canvas_.Save(qb.getValue());
            }
        }
        catch (java.io.IOException ex)
        {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }
    
    boolean CommandSaveEnabled()
    {
        return canvas_.getChanged();
    }
    
    boolean PromptExit(Console console)
    {
        if (canvas_.getChanged())
        {
            if (MsgBox.domodal(console, "Unsaved changes", "Do you wish to exit anyway?", 20) > 0)
                return true;
            else
                return false;
        }
        else
            return true;
    }
    
    @Override
	public void onEvent(ConsoleState state, Event.Key kev)
	{
        super.onEvent(state, kev);
        // Send keys to all children
        for (Window d : getChildren())
        {
            d.onEvent(state, kev);
        }
	}
}
