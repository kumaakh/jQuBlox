package com.mamigo.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;

public class CollectionUtils {
	public static String getPrettyString(Collection<?> coll) {
		StringBuffer sb = new StringBuffer();
		Iterator<?> it = coll.iterator();

		while (it.hasNext()) {
			String s = it.next().toString();
			sb.append(s + "\n");
		}

		return sb.toString().trim();
	}

	public static <T> T only(Collection<T> coll) {
		if (coll.size() != 1) {
			throw new NoSuchElementException("Collection has none or more than one elements");
		}
		return coll.iterator().next();
	}

	public static <T> T first(List<T> list) {
		if (list.size() < 1) {
			throw new NoSuchElementException("Collection does not have a enough elements");
		}
		return list.get(0);
	}

	public static <T> T second(List<T> list) {
		if (list.size() < 2) {
			throw new NoSuchElementException("Collection does not have a enough elements");
		}
		return list.get(1);
	}

	public static <T> T third(List<T> list) {
		if (list.size() < 3) {
			throw new NoSuchElementException("Collection does not have a enough elements");
		}
		return list.get(2);
	}

	public static <T> T fourth(List<T> list) {
		if (list.size() < 4) {
			throw new NoSuchElementException("Collection does not have a enough elements");
		}
		return list.get(3);
	}

	public static <T> T fifth(List<T> list) {
		if (list.size() < 5) {
			throw new NoSuchElementException("Collection does not have a enough elements");
		}
		return list.get(4);
	}

	public static <T> T last(List<T> list) {
		if (list.size() == 0) {
			throw new NoSuchElementException("Collection does not have a enough elements");
		}
		return list.get(list.size() - 1);
	}

	public static <T> boolean isCollection(Class<T> c) {
		return Collection.class.isAssignableFrom(c) || c.isArray();
	}

	public static <T> T atOrNull(List<T> list, int index) {
		if (list.size() <= index)
			return null;
		return list.get(index);
	}

	public static <T> List<T> asList(T... objs) {
		return Arrays.asList(objs);
	}
	public static <T> List<T> asList(Collection<T> coll) {
		return new ArrayList<T>(coll);
	}

	public static <T> List<T> first_nItems(int n, Collection<T> fromList) {
		if (n >= fromList.size())
			return new ArrayList<T>(fromList);
		return (new ArrayList<T>(fromList)).subList(0, n);
	}

	public static <T> Set<T> asSet(T... arr) {
		Set<T> ret = new HashSet<T>();
		for (T t : arr) {
			ret.add(t);
		}
		return ret;
	}
	public static <T> void sortDescending_by(List<T> list, final String property)
	{
		sortAscending_by(list,property);
		Collections.reverse(list);
	}
	
	public static <T> void sortAscending_by(List<T> list, final String property)
	{
		Comparator<T> c= new Comparator<T>()
		{
			@Override
			public int compare(T o1, T o2) {
				try{
					Object f1=BeanUtils.getProperty(o1,property);
					Object f2=BeanUtils.getProperty(o2,property);
					return safeCompare(f1,f2);
				}
				catch(Exception iae)
				{
					throw new RuntimeException("error while sorting",iae);
				}
			}
		};
		Collections.sort(list,c);
	}

	public static  <T> List<T> createSortedAscendingList_by(Collection<T> coll, final String property)
	{
		List<T> list=asList(coll);
		sortAscending_by(list,property);
		return list;
	}
	
	/*
	 * Does not throw any exceptions while comparing
	 * 
	 */
	private static int safeCompare(Object f1, Object f2) {
		if(f1==null && f2==null) return 0;
		if(f1==null && f2!=null) return -1;
		if(f1!=null && f2==null) return 1;
		if(f1.equals(f2)) return 0;
		return safeCompare(f1.toString(), f2.toString());
	}
	public static int safeCompare(String f1, String f2) {
		if(f1==null && f2==null) return 0;
		if(f1==null && f2!=null) return -1;
		if(f1!=null && f2==null) return 1;
		return f1.compareTo(f2);
	}
	public static boolean safeEquals(String f1,String f2)
	{
		return 0==safeCompare(f1, f2);
	}
	public static <T> boolean isEmptyOrNull(Collection<T> c)
	{
		if(null==c) return true;
		return c.isEmpty();
	}
	public static <K,V> Map<K,V> asMap(Collection<V> coll, NoArgsFunctor<K,V> funct)
	{
		Map<K,V> map= new HashMap<K, V>();
		for (V v : coll) {
			map.put(funct.call(v),v);
		}
		return map;
	}
	public static <P,V> List<P> collectFrom_using(Collection<V> coll, NoArgsFunctor<P,V> extractor)
	{
		List<P> ret= new ArrayList<P>();
		for(V v: coll)
		{
			P prop = extractor.call(v);
			if(null!=prop)
			{
				ret.add(prop);
			}
		}
		return ret;
	}
	public static <V> String join(String seprator,Object... arr)
	{
		return join(seprator,asList(arr));
	}
	public static <V> String join(String seprator,Iterable<V> coll)
	{
		return join("",seprator,coll);
	}
	public static <V> String join(String prefix, String seprator,Iterable<V> coll)
	{
		return join(prefix,false,seprator,coll);
	}
	public static <V> String join(String prefix,boolean bQuote ,String seprator,Iterable<V> coll)
	{
		StringBuffer sb= new StringBuffer(prefix);
		for (Iterator<V> iterator = coll.iterator(); iterator.hasNext();) {
			V v= iterator.next();
			if(bQuote) sb.append("'");
			sb.append(v.toString());
			if(bQuote) sb.append("'");
			if(iterator.hasNext())sb.append(seprator);
		}
		return sb.toString();
	}
	public static <T> List<T> emptyList()
	{
		return new ArrayList<T>();
	}

}
