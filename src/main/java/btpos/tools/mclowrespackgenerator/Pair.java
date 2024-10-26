package btpos.tools.mclowrespackgenerator;

public class Pair<T, U> {
	public final T left;
	public final U right;
	
	public T getLeft() {
		return left;
	}
	
	public U getRight() {
		return right;
	}
	
	public Pair(T left, U right) {
		this.left = left;
		this.right = right;
	}
	
	public <V> Pair<T, V> rmap(V v) {
		return new Pair<>(left, v);
	}
	
	public <V> Pair<V, U> lmap(V v) {
		return new Pair<>(v, right);
	}
}
