package au.radsoft.ansidraw;

import au.radsoft.console.CharInfo;
import au.radsoft.console.Color;
import au.radsoft.console.Buffer;

public class Layer implements java.io.Serializable
{
    private static final long serialVersionUID = 7857893L;
    static java.lang.reflect.Field fBuffer_;
    
    static
    {
        try
        {
            fBuffer_ = Layer.class.getDeclaredField("buffer_");
            fBuffer_.setAccessible(true);
        }
        catch (Exception e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }

    static public void save(java.util.ArrayList<Layer> layers, String filename)
        throws java.io.IOException
    {
        java.io.FileOutputStream fos = new java.io.FileOutputStream(filename);
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(fos);
        oos.writeObject(layers);
        oos.close();
    }
    
    @SuppressWarnings("unchecked")  // TODO Is there a way around this?
    static public java.util.ArrayList<Layer> load(String filename)
        throws java.io.IOException, ClassNotFoundException
    {
        java.io.FileInputStream fis = new java.io.FileInputStream(filename);
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(fis);
        java.util.ArrayList<Layer> layers = (java.util.ArrayList<Layer>) ois.readObject();
        ois.close();
        return layers;
    }
    
    public Layer(String name, int ow, int oh)
    {
        name_ = name;
        visible_ = true;
        buffer_ = new Buffer(ow, oh);
        buffer_.fill(0, 0, ow, oh, '\0');
    }

    public String GetName() { return name_; }
    public void SetName(String name) { name_ = name; }
    public Buffer GetBuffer() { return buffer_; }
    
    private void writeObject(java.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        out.writeObject(name_);
        out.writeBoolean(visible_);
        out.writeInt(buffer_.getWidth());
        out.writeInt(buffer_.getHeight());
        for (int y = 0; y < buffer_.getHeight(); ++y)
        {
            for (int x = 0; x < buffer_.getWidth(); ++x)
            {
                CharInfo ci = buffer_.get(x, y);
                out.writeChar(ci.c);
                out.writeObject(ci.fg);
                out.writeObject(ci.bg);
            }
        }
    }
    
    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException
    {
        try
        {
            name_ = (String) in.readObject();
            visible_ = in.readBoolean();
            int ow = in.readInt();
            int oh = in.readInt();
            //buffer_ = new Buffer(ow, oh);
            fBuffer_.set(this, new Buffer(ow, oh));

            for (int y = 0; y < buffer_.getHeight(); ++y)
            {
                for (int x = 0; x < buffer_.getWidth(); ++x)
                {
                    CharInfo ci = buffer_.get(x, y);
                    ci.c = in.readChar();
                    ci.fg = (Color) in.readObject();
                    ci.bg = (Color) in.readObject();
                }
            }
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
    
    private String name_;
    public boolean visible_;
    private final Buffer buffer_;
}
