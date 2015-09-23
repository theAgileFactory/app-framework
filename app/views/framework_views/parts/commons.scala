package views.html.framework_views.parts

import play.api.data.Field

/**
 * This class provides a few scala utilities to be reused in various
 * parts
 */
object commons extends framework.handlers.ViewsInjector{
    /**
     * Check if a field is marked with the "required" constraint
     */
	val isRequired  =  (afield : Field)  =>  {
		var isConstraintsRequired=false;
		if(afield.constraints!=null){
		 	for( constraint <- afield.constraints ){
		 		if(constraint._1.equals("constraint.required")){
		 			isConstraintsRequired=true;
		 		}
		 	}
	 	}
	 	isConstraintsRequired;
	}
}