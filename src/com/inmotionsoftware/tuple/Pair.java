package com.inmotionsoftware.tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Pair<A,B> extends Tuple {

	private final A m_a;
	private final B m_b;
	
	public static <X> Pair<X,X> fromArray(X[] array) {
		return new Pair<X, X>(array[0], array[1]);
	}

	public static <X> Pair<X,X> fromCollection(Collection<X> collection) {
		return fromIterable(collection);
	}
	
	public static <X> Pair<X,X> fromIterator(Iterator<X> it) {
		return new Pair<X,X>(it.next(), it.next());
	}
	
	public static <X> Pair<X,X> fromIterable(Iterable<X> it) {
		return fromIterator(it.iterator());
	}
	
	public Pair(A a, B b) {
		m_a = a;
		m_b = b;
	}
	
	public int indexOf(Object o) {
		if (o.equals(m_a)) return 0;
		if (o.equals(m_b)) return 1;
		return -1;
	}
	
	public int lastIndexOf(Object o) {
		if (o.equals(m_b)) return 1;
		if (o.equals(m_a)) return 0;
		return -1;
	}

	public Object[] toArray() {
		return new Object[] {m_a, m_b};
	}
	
	public Collection<Object> toCollection() {
		ArrayList<Object> list = new ArrayList<>();
		list.add(m_a);
		list.add(m_b);
		return list;
	}
	
	public int hashCode() {
		return m_a.hashCode() ^ m_b.hashCode();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Pair<?,?>)) return false;
		
		Pair<?,?> other = (Pair<?,?>)o;
		return m_a.equals(other.m_a)
			&& m_b.equals(other.m_b);
	}
	
	public boolean contains(Object o) {
		return o == m_a || o == m_b || o.equals(m_a) || o.equals(m_b);
	}
	
	public Object get(int idx) throws IndexOutOfBoundsException {
		switch(idx) {
			case 0:
				return get0();
			case 1:
				return get1();

			default:
				throw new IndexOutOfBoundsException();		
		}		
	}
	
	public int getSize() {
		return 2;
	}

	public A get0() {
		return m_a;
	}
	
	public B get1() {
		return m_b;
	}
}
