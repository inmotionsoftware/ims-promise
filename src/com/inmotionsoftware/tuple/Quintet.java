package com.inmotionsoftware.tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Quintet<A,B,C,D,E> extends Tuple {

	private final A m_a;
	private final B m_b;
	private final C m_c;
	private final D m_d;
	private final E m_e;
	
	public static <X> Quintet<X,X,X,X,X> fromArray(X[] array) {
		return new Quintet<X,X,X,X,X>(array[0], array[1], array[2], array[2], array[3]);
	}

	public static <X> Quintet<X,X,X,X,X> fromCollection(Collection<X> collection) {
		return fromIterable(collection);
	}
	
	public static <X> Quintet<X,X,X,X,X> fromIterator(Iterator<X> it) {
		return new Quintet<X,X,X,X,X>(it.next(), it.next(), it.next(), it.next(), it.next());
	}
	
	public static <X> Quintet<X,X,X,X,X> fromIterable(Iterable<X> it) {
		return fromIterator(it.iterator());
	}
	
	public Quintet(A a, B b, C c, D d, E e) {
		m_a = a;
		m_b = b;
		m_c = c;
		m_d = d;
		m_e = e;
	}
	
	public int indexOf(Object o) {
		if (o.equals(m_a)) return 0;
		if (o.equals(m_b)) return 1;
		if (o.equals(m_c)) return 2;
		if (o.equals(m_d)) return 3;
		if (o.equals(m_e)) return 4;
		return -1;
	}
	
	public int lastIndexOf(Object o) {
		if (o.equals(m_e)) return 3;
		if (o.equals(m_d)) return 3;
		if (o.equals(m_c)) return 2;
		if (o.equals(m_b)) return 1;
		if (o.equals(m_a)) return 0;
		return -1;
	}

	public Object[] toArray() {
		return new Object[] {m_a, m_b, m_c, m_d, m_e};
	}
	
	public Collection<Object> toCollection() {
		ArrayList<Object> list = new ArrayList<>();
		list.add(m_a);
		list.add(m_b);
		list.add(m_c);
		list.add(m_d);
		list.add(m_e);
		return list;
	}
	
	public int hashCode() {
		return m_a.hashCode() ^ m_b.hashCode() ^ m_c.hashCode() ^ m_d.hashCode() ^ m_e.hashCode();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Quintet<?,?,?,?,?>)) return false;
		
		Quintet<?,?,?,?,?> other = (Quintet<?,?,?,?,?>)o;
		return m_a.equals(other.m_a)
			&& m_b.equals(other.m_b)
			&& m_c.equals(other.m_c)
			&& m_d.equals(other.m_d)
			&& m_e.equals(other.m_e);
	}
	
	public boolean contains(Object o) {
		return o == m_a || o == m_b || o == m_c || o == m_d || o == m_e
				|| o.equals(m_a) || o.equals(m_b) || o.equals(m_c) || o.equals(m_d) || o.equals(m_e);
	}
	
	public Object get(int idx) throws IndexOutOfBoundsException {
		switch(idx) {
			case 0:
				return get0();
			case 1:
				return get1();
			case 2:
				return get2();
			case 3:
				return get3();
			case 4:
				return get4();
			default:
				throw new IndexOutOfBoundsException();		
		}		
	}
	
	public int getSize() {
		return 5;
	}

	public A get0() {
		return m_a;
	}
	
	public B get1() {
		return m_b;
	}
	
	public C get2() {
		return m_c;
	}
	
	public D get3() {
		return m_d;
	}
	
	public E get4() {
		return m_e;
	}
}
