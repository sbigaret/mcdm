package pl.poznan.put;

public class Tuple<A, B> {
    private A First;
    private B Second;

    public A getFirst()
    {
        return First;
    }

    public B getSecond()
    {
        return Second;
    }

    public void set(A first, B second)
    {
        First = first;
        Second = second;
    }

    public Tuple(A first, B second)
    {
        this.set(first, second);
    }

}
