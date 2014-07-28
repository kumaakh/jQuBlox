package android.text;

import com.mamigo.util.CollectionUtils;

public class TextUtils {
public static String join(CharSequence seprator, Object[] arr){
	return CollectionUtils.join(seprator.toString(), arr);
}
public static String join(CharSequence seprator, Iterable arr){
	return CollectionUtils.join(seprator.toString(), arr);
}
}
