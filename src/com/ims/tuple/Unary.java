package com.ims.tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Unary<A> extends Tuple {

	private final A m_a;
		
	public static <X> Unary<X> fromArray(X[] array) {
		return new Unary<X>(array[0]);
	}

	public static <X> Unary<X> fromCollection(Collection<X> collection) {
		return fromIterable(collection);
	}
	
	public static <X> Unary<X> fromIterator(Iterator<X> it) {
		return new Unary<X>(it.next());
	}
	
	public static <X> Unary<X> fromIterable(Iterable<X> it) {
		return fromIterator(it.iterator());
	}
	
	public Unary(A a) {
		m_a = a;
	}
	
	public int indexOf(Object o) {
		return (o.equals(m_a)) ? 0 : -1;
	}
	
	public int lastIndexOf(Object o) {
		return (o.equals(m_a)) ? 0 : -1;
	}

	public Object[] toArray() {
		return new Object[] {m_a};
	}
	
	public Collection<Object> toCollection() {
		ArrayList<Object> list = new ArrayList<>();
		list.add(m_a);
		return list;
	}
	
	public int hashCode() {
		return m_a.hashCode();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Unary<?>)) return false;
		Unary<?> other = (Unary<?>)o;
		return m_a.equals(other.m_a);
	}
	
	public boolean contains(Object o) {
		return o == m_a || o.equals(m_a);
	}
	
	public Object get(int idx) throws IndexOutOfBoundsException {
		switch(idx) {
			case 0:
				return get0();
			default:
				throw new IndexOutOfBoundsException();		
		}		
	}
	
	public int getSize() {
		return 1;
	}

	public A get0() {
		return m_a;
	}
}
