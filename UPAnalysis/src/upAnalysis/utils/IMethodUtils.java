package upAnalysis.utils;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public class IMethodUtils 
{
	public static String nameWithType(IMethod method)
	{
		return method.getDeclaringType().getElementName() + "." + method.getElementName() + "()";
	}
	
	public static String fullyQualifiedName(IMethod method)
	{
		return method.getDeclaringType().getFullyQualifiedName()  + "." + method.getElementName() + "()";
	}
	
	public static String partiallyQualifiedNameWithParamDots(IMethod method)
	{
		return method.getDeclaringType().getElementName() + "." + unqualifiedNameWithParamDots(method);
	}	
	
	public static String unqualifiedNameWithParamDots(IMethod method)
	{
		StringBuilder result = new StringBuilder();
		result.append(method.getElementName() + "(");
		for (int i = 0; i < method.getNumberOfParameters(); i++)
			result.append("..,");
		
		// Get rid of the last comma
		if (method.getNumberOfParameters() > 0)
			result.deleteCharAt(result.length() - 1);
		result.append(")");
		return result.toString(); 
	}
	
	public static String returnType(IMethod method)
	{
		try {
			return Signature.getSignatureSimpleName(method.getReturnType());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";		
	}
}
