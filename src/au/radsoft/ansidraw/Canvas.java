package au.radsoft.ansidraw;

import au.radsoft.textui.*;

import au.radsoft.console.CharInfo;
import au.radsoft.console.Color;
import au.radsoft.console.Console;
import au.radsoft.console.Event;
import au.radsoft.console.Buffer;

import java.util.ArrayList;
import java.util.ListIterator;

// TODO
// Draw centred
// Scrollbars

class Canvas extends ScrollFrameWindow
{
    static java.lang.reflect.Field fLayers_ = getField(Canvas.class, "layers_");
    
    static java.lang.reflect.Field getField(Class<?> c, String name)
    {
        try
        {
            java.lang.reflect.Field field = c.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
        catch (Exception e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    
    interface Watch
    {
        enum Section { LAYERS, BUFFER };
        void changedCanvas(Section s);
    }
    
    public Canvas(int ox, int oy, int ow, int oh, Options o, StatusBar sb)
    {
        super(ox, oy, ow, oh);
        setTitle("Untitled");
        New(ow, oh);
        o_ = o;
        sb_ = sb;
    }
    
    void New(int ow, int oh)
    {
        layers_.clear();
        activeLayer_ = new Layer("Layer 1", ow, oh);
        layers_.add(activeLayer_);
        sx_ = 0;
        sy_ = 0;
        sw_ = ow;
        sh_ = oh;
        invalid_ = true;
    }
    
    String getFilename()
    {
        return filename_;
    }
    
    boolean getChanged()
    {
        return changed_;
    }
    
    int GetOffSetX()
    {
        Buffer aw = activeLayer_.GetBuffer();
        if (aw.getWidth() < w_)
            return (w_ - aw.getWidth()) / 2;
        else
            return 0;
    }
    
    int GetOffSetY()
    {
        Buffer aw = activeLayer_.GetBuffer();
        if (aw.getHeight() < h_)
            return (h_ - aw.getHeight()) / 2;
        else
            return 0;
    }
    
    @Override
    protected void draw(Buffer w, int wx, int wy)
    {
        super.draw(w, wx, wy);
        
        final Buffer aw = activeLayer_.GetBuffer();
        
        final int vw = Math.min(aw.getWidth(), w_);
        final int vh = Math.min(aw.getHeight(), h_);
        
        final int wox = GetOffSetX();
        final int woy = GetOffSetY();
        
        for (int xx = 0; xx < w_; ++xx) {
            for (int yy = 0; yy < h_; ++yy) {
                final CharInfo dstcell = w.get(xx + wx, yy + wy);
                if (dstcell != null) {
                    if (xx >= wox && xx < (aw.getWidth() + wox) && yy >= woy && yy < (aw.getHeight() + woy))
                    {
                        //dstcell.c = (xx + yy) % 2 == 0 ? (char) 177 : ' ';
                        dstcell.c = (char) 177;
                        //dstcell.fg = Color.GRAY;
                        dstcell.fg = (xx + yy) % 2 == 0 ? Color.LIGHT_GRAY : Color.GRAY;
                        dstcell.bg = Color.BLACK;
                    }
                    else
                    {
                        dstcell.c = ' ';
                    }
                }
            }
        }
        for (ListIterator<Layer> iterator = layers_.listIterator(layers_.size()); iterator.hasPrevious();)
        {
            final Layer layer = iterator.previous();
            if (layer.visible_)
                write(w, wx + wox, wy + woy, layer.GetBuffer(), sx_, sy_, vw, vh);
        }
        //w.write(wx, wy, GetActiveLayer().GetBuffer());
        o_.tool_.draw(this, w, wox, woy, sx_, sy_, vw, vh);
    }
    
    @Override
    public void onEvent(ConsoleState state, Event.Key kev)
    {
        super.onEvent(state, kev);
        
        if (o_.tool_.OnEvent(this, state, kev))
            invalid_ = true;
    }
    
    @Override
    public void onEvent(ConsoleState state, Event.MouseButton mbev)
    {
        super.onEvent(state, mbev);
        
        if (insideClientArea(mbev.mx, mbev.my))
        {
            Event.MouseButton lmbev = new Event.MouseButton(mbev.key, mbev.state, mbev.mx + sx_, mbev.my + sy_);
            if (o_.tool_.OnEvent(this, state, lmbev))
                invalid_ = true;
        }
    }
    
    @Override
    public void onEvent(ConsoleState state, Event.MouseMoved mmev)
    {
        super.onEvent(state, mmev);
        
        if (insideClientArea(mmev.mx, mmev.my))
        {
            Event.MouseMoved lmmev = new Event.MouseMoved(mmev.mx + sx_, mmev.my + sy_);
            if (o_.tool_.OnEvent(this, state, lmmev))
                invalid_ = true;
            sb_.set(String.format("X: %2d Y: %2d", lmmev.mx, lmmev.my));
        }
        else
        {
            if (o_.tool_.OnLeave())
                invalid_ = true;
            sb_.set(null);
        }
    }
    
    @Override
    public void onMouseEnter(ConsoleState state)
    {
        super.onMouseEnter(state);
        //System.err.println("MouseEnter");
    }
    
    @Override
    public void onMouseLeave(ConsoleState state)
    {
        super.onMouseLeave(state);
        //System.err.println("MouseLeave");
        if (o_.tool_.OnLeave())
            invalid_ = true;
        sb_.set(null);
    }
    
    /*private*/ static void write(Buffer dstb, int dx, int dy, Buffer srcb, int sx, int sy, int sw, int sh) {
        for (int xx = 0; xx < sw; ++xx) {
            for (int yy = 0; yy < sh; ++yy) {
                final CharInfo dstcell = dstb.get(xx + dx, yy + dy);
                if (dstcell != null) {
                    final CharInfo srccell = srcb.get(xx + sx, yy + sy);
                    if (srccell != null && srccell.c != '\0') {
                        dstcell.set(srccell);
                    }
                }
            }
        }
    }
    
    Options GetOptions() { return o_; }
    Layer GetActiveLayer() { return activeLayer_; }
    ArrayList<Layer> GetLayers() { return layers_; }
    
    void SetActiveLayer(Layer activeLayer)
    {
        activeLayer_ = activeLayer;
        changed(Watch.Section.LAYERS);
    }
    
    void SetLayerVisiblity(Layer l, boolean v)
    {
        l.visible_ = v;
        invalid_ = true;
        changed(Watch.Section.LAYERS);
    }
    
    void MoveLayerUp(Layer l)
    {
        int i = layers_.indexOf(l);
        if (i < 0)
            throw new IllegalArgumentException("Layer not in canvas.");
        if (i > 0)
        {
            layers_.remove(i);
            layers_.add(i - 1, l);
            changed(Watch.Section.LAYERS);
            invalid_ = true;
        }
    }
    
    void MoveLayerDown(Layer l)
    {
        int i = layers_.indexOf(l);
        if (i < 0)
            throw new IllegalArgumentException("Layer not in canvas.");
        if (i < (layers_.size() - 1))
        {
            layers_.remove(i);
            layers_.add(i + 1, l);
            changed(Watch.Section.LAYERS);
            invalid_ = true;
        }
    }
    
    void DeleteLayer(Layer l)
    {
        int i = layers_.indexOf(l);
        if (i < 0)
            throw new IllegalArgumentException("Layer not in canvas.");
        layers_.remove(i);
        if (l == activeLayer_)
            activeLayer_ = layers_.get(i);
        changed(Watch.Section.LAYERS);
        invalid_ = true;
    }
    
    void AddLayer()
    {
        layers_.add(new Layer("Layer " + ++layerCount_, activeLayer_.GetBuffer().getWidth(), activeLayer_.GetBuffer().getHeight()));
        changed(Watch.Section.LAYERS);
    }
    
    void Save(String filename)
        throws java.io.IOException
    {
        filename_ = filename;
        setTitle(filename_);
        Layer.save(layers_, filename + ".ansidraw");
        changed_ = false;
    }
    
    //@SuppressWarnings("unchecked")  // TODO Is there a way around this?
    void Load(String filename)
        throws java.io.FileNotFoundException, java.io.IOException, ClassNotFoundException
    {
        try
        {
            filename_ = filename;
            setTitle(filename_);
            fLayers_.set(this, Layer.load(filename + ".ansidraw"));
            activeLayer_ = layers_.get(0);
            layerCount_ = layers_.size() + 1;
            changed(Watch.Section.LAYERS);
            invalid_ = true;
            changed_ = false;
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
    
    void changed(Watch.Section s)
    {
        changed_ = true;
        for (Watch w : watchers_)
            w.changedCanvas(s);
    }
    
    private String filename_;
    private Layer activeLayer_;
    private int layerCount_ = 1;
    private final ArrayList<Layer> layers_ = new ArrayList<Layer>();
    private final Options o_;
    private final StatusBar sb_;
    private boolean changed_ = false;
    
    java.util.Vector<Watch> watchers_ = new java.util.Vector<Watch>();
}
