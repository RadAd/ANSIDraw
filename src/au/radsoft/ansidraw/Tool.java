package au.radsoft.ansidraw;

import au.radsoft.textui.*;

import au.radsoft.console.CharInfo;
import au.radsoft.console.CharKey;
import au.radsoft.console.Color;
import au.radsoft.console.Console;
import au.radsoft.console.Event;
import au.radsoft.console.Buffer;

// http://problem4me.wordpress.com/2007/07/28/midpoint-ellipse-java-code/

interface Tool
{
    void draw(Canvas canvas, Buffer w, int wox, int woy, int sx, int sy, int sw, int sh);
    boolean OnEvent(Canvas canvas, ConsoleState state, Event.Key kev);
    boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseButton mbev);
    boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseMoved mmev);
    boolean OnLeave();
    
    static abstract class BaseTool implements Tool
    {
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.Key kev)
        {
            return false;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseButton mbev)
        {
            return false;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseMoved mmev)
        {
            boolean invalid = false;
            
            valid_ = true;
            wx_ = mmev.mx - canvas.ox_;
            wy_ = mmev.my - canvas.oy_;
            invalid = true;
            
            return invalid;
        }
        
        @Override
        public boolean OnLeave()
        {
            boolean invalid = valid_;
            valid_ = false;
            return invalid;
        }
        
        protected boolean valid_ = false;
        protected int wx_;
        protected int wy_;
    }
    
    static abstract class Base2Tool implements Tool
    {
        abstract boolean Commit(Canvas canvas);
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.Key kev)
        {
            boolean invalid = false;
            
            switch (kev.key)
            {
            case ESCAPE:
                if (kev.state == Event.State.PRESSED)
                {
                    validstart_ = false;
                    validend_ = false;
                    invalid = true;
                }
                break;
                
            case SHIFT:
                if (kev.state == Event.State.PRESSED && !filled_)
                {
                    filled_ = true;
                    invalid = true;
                }
                if (kev.state == Event.State.RELEASED && filled_)
                {
                    filled_ = false;
                    invalid = true;
                }
                break;
            }

            return invalid;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseButton mbev)
        {
            boolean invalid = false;
            
            switch (mbev.key)
            {
            case MOUSE_BUTTON1:
                if (mbev.state == Event.State.PRESSED)
                {
                    validstart_ = true;
                    validend_ = true;
                    wsx_ = mbev.mx - canvas.ox_;
                    wsy_ = mbev.my - canvas.oy_;
                    wex_ = wsx_;
                    wey_ = wsy_;
                    filled_ = state.getKeyDown(CharKey.SHIFT);
                    invalid = true;
                }
                else if (mbev.state == Event.State.RELEASED)
                {
                    filled_ = state.getKeyDown(CharKey.SHIFT);
                    if (validend_ && Commit(canvas))
                    {
                        canvas.changed(Canvas.Watch.Section.BUFFER);
                        validstart_ = false;
                        validend_ = false;
                        invalid = true;
                    }
                }
                break;
            }

            return invalid;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseMoved mmev)
        {
            boolean invalid = false;
            
            if (state.getKeyDown(CharKey.MOUSE_BUTTON1))
            {
                validend_ = true;
                wex_ = mmev.mx - canvas.ox_;
                wey_ = mmev.my - canvas.oy_;
                filled_ = state.getKeyDown(CharKey.SHIFT);
                invalid = true;
            }
            
            return invalid;
        }
            
        @Override
        public boolean OnLeave()
        {
            boolean invalid = validstart_;
            validstart_ = false;
            validend_ = false;
            return invalid;
        }
        
        protected boolean validstart_ = false;
        protected int wsx_;
        protected int wsy_;
        protected boolean validend_ = false;
        protected int wex_;
        protected int wey_;
        protected boolean filled_ = true;
    }
    
    static class Pen extends BaseTool
    {
        @Override
        public void draw(Canvas canvas, Buffer w, int wox, int woy, int sx, int sy, int sw, int sh)
        {
            Options o = canvas.GetOptions();
            if (valid_ && wx_ >= wox && wy_ >= woy)
                w.write(wx_ - sx + canvas.ox_, wy_ - sy + canvas.oy_, o.getChar(), o.fg_, o.bg_);
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseButton mbev)
        {
            boolean invalid = super.OnEvent(canvas, state, mbev);
            
            Options o = canvas.GetOptions();
            Buffer w = canvas.GetActiveLayer().GetBuffer();
            int wox = canvas.GetOffSetX();
            int woy = canvas.GetOffSetY();

            if (mbev.state == Event.State.PRESSED)
            {
                switch (mbev.key)
                {
                case MOUSE_BUTTON1:
                    w.write(wx_ - wox, wy_ - woy, o.getChar(), o.fg_, o.bg_);
                    canvas.changed(Canvas.Watch.Section.BUFFER);
                    invalid = true;
                    break;
                }
            }

            return invalid;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseMoved mmev)
        {
            boolean invalid = super.OnEvent(canvas, state, mmev);
            
            Options o = canvas.GetOptions();
            Buffer w = canvas.GetActiveLayer().GetBuffer();
            int wox = canvas.GetOffSetX();
            int woy = canvas.GetOffSetY();
            
            if (state.getKeyDown(CharKey.MOUSE_BUTTON1))
            {
                w.write(wx_ - wox, wy_ - woy, o.getChar(), o.fg_, o.bg_);
                canvas.changed(Canvas.Watch.Section.BUFFER);
                invalid = true;
            }
            
            return invalid;
        }
    }
    
    static class Fill extends BaseTool
    {
        @Override
        public void draw(Canvas canvas, Buffer w, int wox, int woy, int sx, int sy, int sw, int sh)
        {
            Options o = canvas.GetOptions();
            if (valid_ && wx_ >= wox && wy_ >= woy)
                w.write(wx_ - sx + canvas.ox_, wy_ - sy + canvas.oy_, o.getChar(), o.fg_, o.bg_);
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseButton mbev)
        {
            boolean invalid = super.OnEvent(canvas, state, mbev);
            
            Options o = canvas.GetOptions();
            Buffer w = canvas.GetActiveLayer().GetBuffer();

            if (mbev.state == Event.State.RELEASED)
            {
                switch (mbev.key)
                {
                case MOUSE_BUTTON1:
                    CharInfo co = new CharInfo(w.get(wx_, wy_));
                    if (co.c != o.getChar() || co.fg != o.fg_ || co.bg != o.bg_)
                    {
                        Fill(w, o, co, wx_, wy_);
                        canvas.changed(Canvas.Watch.Section.BUFFER);
                        invalid = true;
                    }
                    break;
                }
            }
            return invalid;
        }
        
        private void Fill(Buffer w, Options o, CharInfo co, int x, int y)
        {
            CharInfo c = w.get(x, y);
            if (c != null && c.equals(co))
            {
                c.c = o.getChar();
                c.fg = o.fg_;
                c.bg = o.bg_;
                
                Fill(w, o, co, x - 1, y);
                Fill(w, o, co, x, y - 1);
                Fill(w, o, co, x + 1, y);
                Fill(w, o, co, x, y + 1);
            }
        }
    }
    
    static class Square extends Base2Tool
    {
        @Override
        public void draw(Canvas canvas, Buffer w, int wox, int woy, int sx, int sy, int sw, int sh)
        {
            Options o = canvas.GetOptions();
            if (validend_)
                DrawSquare(w, o, canvas.ox_ - sx, canvas.oy_ - sy, filled_);
            else
                w.write(wsx_ - sx + canvas.ox_, wsy_ - sx + canvas.oy_, o.getChar(), o.fg_, o.bg_);
        }
        
        private void DrawSquare(Buffer w, Options o, int ox , int oy, boolean filled)
        {
            int minx = Math.min(wsx_, wex_) + ox;
            int maxx = Math.max(wsx_, wex_) + ox;
            int miny = Math.min(wsy_, wey_) + oy;
            int maxy = Math.max(wsy_, wey_) + oy;
            
            if (o.getChar() == (char) 196 || o.getChar() == (char) 179)
            {
                Dialog.drawSquareSingle(w, minx, miny, maxx - minx + 1, maxy - miny + 1, o.fg_, o.bg_);
                if (filled_)
                    w.fill(minx + 1, miny + 1, maxx - minx - 1, maxy - miny - 1, ' ', o.fg_, o.bg_);
            }
            else if (o.getChar() == (char) 186 || o.getChar() == (char) 205)
            {
                Dialog.drawSquareDouble(w, minx, miny, maxx - minx + 1, maxy - miny + 1, o.fg_, o.bg_);
                if (filled_)
                    w.fill(minx + 1, miny + 1, maxx - minx - 1, maxy - miny - 1, ' ', o.fg_, o.bg_);
            }
            else
            {
                if (filled)
                {
                    w.fill(minx, miny, maxx - minx + 1, maxy - miny + 1, o.getChar(), o.fg_, o.bg_);
                }
                else
                {
                    for (int xx = minx; xx < maxx; ++xx)
                    {
                        w.write(xx, miny, o.getChar(), o.fg_, o.bg_);
                        w.write(xx + 1, maxy, o.getChar(), o.fg_, o.bg_);
                    }
                    for (int yy = miny; yy < maxy; ++yy)
                    {
                        w.write(minx, yy + 1, o.getChar(), o.fg_, o.bg_);
                        w.write(maxx, yy, o.getChar(), o.fg_, o.bg_);
                    }
                }
            }
        }
        
        @Override
        public boolean Commit(Canvas canvas)
        {
            Options o = canvas.GetOptions();
            Buffer w = canvas.GetActiveLayer().GetBuffer();
            
            DrawSquare(w, o, 0, 0, filled_);
            return true;
        }
    }
    
    static class Line extends Base2Tool
    {
        @Override
        public void draw(Canvas canvas, Buffer w, int wox, int woy, int sx, int sy, int sw, int sh)
        {
            Options o = canvas.GetOptions();
            if (validend_)
                DrawLine(w, o, canvas.ox_ - sx, canvas.oy_ - sy);
            else
                w.write(wsx_ - sx + canvas.ox_, wsy_ - sy + canvas.oy_, o.getChar(), o.fg_, o.bg_);
        }
        
        private void DrawLine(Buffer w, Options o, int ox , int oy)
        {
            int x0 = wsx_ + ox;
            int x1 = wex_ + ox;
            int y0 = wsy_ + oy;
            int y1 = wey_ + oy;
            
            int dx = Math.abs(x1 - x0);
            int dy = Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int err = dx-dy;

            while (true)
            {
                w.write(x0, y0, o.getChar(), o.fg_, o.bg_);
                if (x0 == x1 && y0 == y1)
                    break;
                int e2 = 2 * err;
                if (e2 > -dy)
                {
                    err = err - dy;
                    x0 = x0 + sx;
                }
                if (e2 < dx)
                {
                    err = err + dx;
                    y0 = y0 + sy;
                }
            }
        }
        
        @Override
        public boolean Commit(Canvas canvas)
        {
            Options o = canvas.GetOptions();
            Buffer w = canvas.GetActiveLayer().GetBuffer();
            
            DrawLine(w, o, 0, 0);
            return true;
        }
    }
    
    static class Select extends Base2Tool
    {
        @Override
        public void draw(Canvas canvas, Buffer w, int wox, int woy, int sx, int sy, int sw, int sh)
        {
            Options o = canvas.GetOptions();
            if (validend_)
                DrawSquare(w, o, canvas.ox_ - sx, canvas.oy_ - sy);
            else
                w.fill(wsx_ - sx + canvas.ox_, wsy_ - sy + canvas.oy_, 1, 1, Color.BLACK, Color.LIGHT_GRAY);
        }
        
        private void DrawSquare(Buffer w, Options o, int ox , int oy)
        {
            int minx = Math.min(wsx_, wex_);
            int maxx = Math.max(wsx_, wex_);
            int miny = Math.min(wsy_, wey_);
            int maxy = Math.max(wsy_, wey_);
            w.fill(minx + ox, miny + oy, maxx - minx + 1, maxy - miny + 1, Color.BLACK, Color.LIGHT_GRAY);
        }
        
        private void SaveToClipboard(Canvas canvas)
        {
            if (validend_)
            {
                int minx = Math.min(wsx_, wex_);
                int maxx = Math.max(wsx_, wex_);
                int miny = Math.min(wsy_, wey_);
                int maxy = Math.max(wsy_, wey_);
                clipboard_ = new Buffer(maxx - minx + 1, maxy - miny + 1);
                Buffer w = canvas.GetActiveLayer().GetBuffer();
                w.read(minx, miny, clipboard_);
            }
        }
        
        private boolean PasteFromClipboard(Canvas canvas)
        {
            boolean invalid = false;
            if (validend_ && clipboard_ != null)
            {
                int minx = Math.min(wsx_, wex_);
                int maxx = Math.max(wsx_, wex_);
                int miny = Math.min(wsy_, wey_);
                int maxy = Math.max(wsy_, wey_);
                Buffer w = canvas.GetActiveLayer().GetBuffer();
                //w.write(minx, miny, clipboard_);
                Canvas.write(w, minx, miny, clipboard_, 0, 0, clipboard_.getWidth(), clipboard_.getHeight());
                invalid = true;
            }
            return invalid;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.Key kev)
        {
            boolean invalid = super.OnEvent(canvas, state, kev);
            
            if (kev.state == Event.State.PRESSED)
            {
                //System.err.println("key pressed: " + kev.key + " " + state.getKeyDown(CharKey.CONTROL));
                switch (kev.key)
                {
                case C:
                    if (state.getKeyDown(CharKey.CONTROL))
                    {
                        SaveToClipboard(canvas);
                    }
                    break;
                    
                case V:
                    if (state.getKeyDown(CharKey.CONTROL))
                    {
                        invalid = PasteFromClipboard(canvas);
                    }
                    break;
                }
            }

            return invalid;
        }
        
        @Override
        public boolean OnLeave()
        {
            return false;
        }
        
        @Override
        public boolean Commit(Canvas canvas)
        {
            return false;
        }
        
        private Buffer clipboard_ = null;
    }
    
    static class Pick implements Tool
    {
        @Override
        public void draw(Canvas canvas, Buffer w, int wox, int woy, int sx, int sy, int sw, int sh)
        {
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.Key kev)
        {
            return false;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseButton mbev)
        {
            boolean invalid = false;
            
            Options o = canvas.GetOptions();
            Buffer w = canvas.GetActiveLayer().GetBuffer();
            
            int wx = mbev.mx - canvas.ox_;
            int wy = mbev.my - canvas.oy_;
            
            if (mbev.state == Event.State.PRESSED)
            {
                switch (mbev.key)
                {
                case MOUSE_BUTTON1:
                    CharInfo ci = w.get(wx, wy);
                    o.setChar(ci.c);
                    o.fg_ = ci.fg;
                    o.bg_ = ci.bg;
                    // TODO redraw color selection, char selection Buffers
                    break;
                }
            }

            return invalid;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseMoved mmev)
        {
            return false;
        }
        
        @Override
        public boolean OnLeave()
        {
            return false;
        }
    }
    
    static class Erase implements Tool
    {
        @Override
        public void draw(Canvas canvas, Buffer w, int wox, int woy, int sx, int sy, int sw, int sh)
        {
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.Key kev)
        {
            return false;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseButton mbev)
        {
            boolean invalid = false;
            
            Buffer w = canvas.GetActiveLayer().GetBuffer();
            
            int wx = mbev.mx - canvas.ox_;
            int wy = mbev.my - canvas.oy_;
            
            if (mbev.state == Event.State.PRESSED)
            {
                switch (mbev.key)
                {
                case MOUSE_BUTTON1:
                    w.write(wx, wy, '\0', Color.WHITE, Color.BLACK);
                    canvas.changed(Canvas.Watch.Section.BUFFER);
                    invalid = true;
                    break;
                }
            }

            return invalid;
        }
        
        @Override
        public boolean OnEvent(Canvas canvas, ConsoleState state, Event.MouseMoved mmev)
        {
            boolean invalid = false;
            
            Buffer w = canvas.GetActiveLayer().GetBuffer();
            
            int wx = mmev.mx - canvas.ox_;
            int wy = mmev.my - canvas.oy_;
            
            if (state.getKeyDown(CharKey.MOUSE_BUTTON1))
            {
                w.write(wx, wy, '\0', Color.WHITE, Color.BLACK);
                invalid = true;
            }
            
            return invalid;
        }
        
        @Override
        public boolean OnLeave()
        {
            return false;
        }
    }
}
