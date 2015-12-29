// ==ClosureCompiler==
// @compilation_level SIMPLE_OPTIMIZATIONS
// ==/ClosureCompiler==

/*
A function which displays a message box to confirm an action
 */
function maf_confirmAction(message){
    return window.confirm(message);
}

/*
 * A simple password rating function
 * 0 : means weak password
 * 1 : means average
 * 2 : means OK (but may be not sufficient still)
 */
function maf_password_rating(password){
    
    if(password.length<8){
        return 0;
    }
    var categories=0;
    if(password.match("[a-z]+")){
        categories++;
    }
    if(password.match("[A-Z]+")){
        categories++;
    }
    if(password.match("[0-9]+")){
        categories++;
    }
    if(password.match("[`~!@#\\$%\\^&\\*\\(\\)_\\-\\+=\\{\\}\\[\\]\\\\\\|:;\"'<>,\\.\\?\\/]+")){
        categories++;
    }
    if(categories<2){
        return 0;
    }
    if(categories>2){
        return 2;
    }
    return 1; 
}

/*
 * Format a number with minimal number of decimals.
 * if minDecimals = 1
 * 2 => 2.0
 * 2.8 => 2.8
 * 2.95 => 2.95
 */
function maf_formatNumber(number, minDecimals) {
    return number.toFixed(Math.max(minDecimals, (number.toString().split('.')[1] || []).length));
}

/*
Function to perform a JSON post and get an HTML part as a response.
url : the URL API
data : the input JSON data (to be stringified)
ifDoneFunction : function to be called if the call is successful (data is passed as a parameter)
ifFailFunction : function to be called if the call fails
 */
function maf_performPostJsonReceiveHtml(url, data, ifDoneFunction, ifFailFunction){
    maf_performPost('application/json; charset=utf-8', 'html', url, data, ifDoneFunction, ifFailFunction)
}

/*
Function to perform a JSON post and get a JSON structure as a response.
url : the URL API
data : the input JSON data (to be stringified)
ifDoneFunction : function to be called if the call is successful (data is passed as a parameter)
ifFailFunction : function to be called if the call fails
 */
function maf_performPostJsonReceiveJson(url, data, ifDoneFunction, ifFailFunction){
    maf_performPost('application/json; charset=utf-8', 'json', url, data, ifDoneFunction, ifFailFunction)
}

/*
Function to perform a post and get a response.
contentType: MIME type of the data sent
dataType: the expected response data type xml, json, script, or html
url : the URL API
data : the input JSON data (to be stringified)
ifDoneFunction : function to be called if the call is successful (data is passed as a parameter)
ifFailFunction : function to be called if the call fails
 */
function maf_performPost(contentType, dataType, url, data, ifDoneFunction, ifFailFunction){
       $.ajax({
        type: "POST",
        dataType: dataType,
        contentType: contentType,
        url: url,
        data: data
    }).done(ifDoneFunction).fail(ifFailFunction);
}

/*
Load some values in a select component
selectId : the id in the select
data : an association list of ValueHolders (see framework utils)
 */
function maf_load_value_holders_in_select(selectId, data){  
    var array = $.map(data, function(value, index) {
        return [value];
    });
    
    array.sort(maf_sort_value_holder);
    
    $('#'+selectId).html('');
    for (var i = 0; i < array.length; i++) {
        var valueHolder=array[i];
        $('#'+selectId).append($('<option>', { 
            value: valueHolder.value,
            text : valueHolder.name 
        }));
    }
    

}

function maf_sort_value_holder(a, b) {
    if ( a.name.toLowerCase() < b.name.toLowerCase() )
        return -1;
    if ( a.name.toLowerCase() > b.name.toLowerCase() )
        return 1;
    return 0;
}

/*
Perform a diff between two associatives arrays
selectableValues : an association list of ValueHolders (see framework utils)
selectedValues : an association list of ValueHolders (see framework utils)
 */
function maf_diff_valueholders_collection(selectableValues, selectedValues){
    var resultingCollection={};
    for(var key in selectableValues){
        var valueHolder=selectableValues[key];
        if(!selectedValues[key]){
            resultingCollection[valueHolder.value]=valueHolder;
        }
    }
    return resultingCollection;
}

/*
Extract a parameter value from the current query string
name : the query parameter name
 */
function maf_queryString(name){
    var result = null;
    var regexS = "[\\?&#]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec('?'+window.location.href.split('?')[1]);
    if(results != null){
        result = decodeURIComponent(results[1].replace(/\+/g, " "));
    }
    return result;
};

/*
Display or hide the errors after the form submit
formName : the name of the form
errors : a JSON object containing a dictionary of the errors (key = fieldName)
formErrorTitle : the message that is displayed at the top of the form in case of error
*/
function maf_displayErrors(formName, errors, formErrorTitle){
    $('#'+formName+' [id*="_field"] .help-inline').html("");
     if(!$.isEmptyObject(errors)){
        $("#"+formName+"_errors").css("display","inline");
        $("#"+formName+"_errors strong").html(formErrorTitle);
        for(fieldName in errors){
            $("#"+formName+" #"+fieldName+"_field .help-inline").html(errors[fieldName]);
        }
    }else{
        $('#'+formName+' [id*="_field"] .help-inline').html("");
        $("#"+formName+"_errors").css("display","none");
        $("#"+formName+"_errors strong").html("");
    }
}

/*
Display an error message in the flash zone
*/
function maf_flashErrorMessage(message){
    maf_flashMessage(message, 'alert-error');
}

/*
Display a success message in the flash zone
*/
function maf_flashSuccessMessage(message){
    maf_flashMessage(message, 'alert-success');
}

/*
Clear flash zone (empty the flash zone from any message)
*/
function maf_clearFlash(message){
    maf_flashHtmlContent('');
}

/*
Display a message in the flash zone.
Use the specified CSS class for the message div.
*/
function maf_flashMessage(message, clazz){
    maf_flashHtmlContent('<div class="alert '+clazz+'">'+message+'</div>');
}

/*
Insert an HTML content in the flash zone
(<div id="maf-flash-message"></div>)
*/
function maf_flashHtmlContent(html){
    $('#maf-flash-message').html(html);
}

/*
 * Sort a table column.
 * This function is called by the table generated by utils.Table
 * sortingFunction : one of the sorting functions (stringSort, numberSort, dateSort)
 * column : a reference to the th tag of a header
 * tableId : the id of the table
 */
function maf_sort(sortingFunctionName,column, tableId){
     var sortingFunction=window[sortingFunctionName];
     var jColumn=$(column);
     
     //Ascending/Descending
    var iconHolder=jColumn.find('i[class*="fa"]');
    var ascending=(iconHolder.attr('class')=='fa fa-chevron-down');
     
     var columnIndex=$("#"+tableId+" th").index(jColumn);
     //Create a dictionary of values for the specified column
     var rows=$("#"+tableId+" tr");
     var cellsArray=[];
     //Get the corresponding columns
     rows.each(function(index){
         var rowId=$(this).attr('id');
         if(rowId){
              var cell=new Object();
              cell.id=rowId;
              var cellValue=$(this).find("td:eq("+columnIndex+")").text();
              cell.value=cellValue;
              cellsArray.push(cell);
         }
     });
     
     //first sort the columns by id to avoid side effect
     //thanks this, we assure the sort is always the same
     cellsArray.sort(function(cellA, cellB){
         var valueA=cellA.id.toLowerCase();
         var valueB=cellB.id.toLowerCase();
         if (valueA < valueB)
          return -1;
         if (valueA > valueB)
          return 1;
         return 0;
     });
     
     //Sort the columns
     cellsArray.sort(function(cellA, cellB){
            return sortingFunction(cellA, cellB);
     });
     if(ascending)
         cellsArray.reverse();
     //Reorder the rows
     var tableBody=$("#"+tableId+" tbody");
     for (var i = 0; i < cellsArray.length; i++){
         var cell=cellsArray[i];
         var row=$("#"+cell.id);
         row.remove();
         tableBody.prepend(row);
     }
     //Switch the icon
     if(ascending){
         iconHolder.removeClass('fa-chevron-down').addClass('fa-chevron-up');
     }else{
         iconHolder.removeClass('fa-chevron-up').addClass('fa-chevron-down');
     }
 }
 function maf_stringSort(cellA, cellB){
     var valueA=cellA.value.toLowerCase();
     var valueB=cellB.value.toLowerCase();
     if (valueA < valueB)
      return -1;
     if (valueA > valueB)
      return 1;
     return 0;
 }
 function maf_numberSort(cellA, cellB){
    return cellA.value - cellB.value;
}
 function maf_dateSort(cellA, cellB){
    try {

        dateA = cellA.value.split("/");
        var valueA = new Date(dateA[2], dateA[1] - 1, dateA[0]);
        
        dateB = cellB.value.split("/");
        var valueB = new Date(dateB[2], dateB[1] - 1, dateB[0]);

    } catch(err) {

        var valueA=new Date(cellA.value);
        var valueB=new Date(cellB.value);

    }

    return valueA-valueB;
 }
 
 function maf_dateAndTimeSort(cellA, cellB){
        try {
            dateA = cellA.value.split("/");
            dayHoursMinA = dateA[2].split(" ");
            hoursMinA=dayHoursMinA[1].split(":");
            var valueA = new Date(dayHoursMinA[0], dateA[1] - 1, dateA[0], hoursMinA[0], hoursMinA[1],0,0);
            
            dateB = cellB.value.split("/");
            dayHoursMinB = dateB[2].split(" ");
            hoursMinB=dayHoursMinB[1].split(":");
            var valueB = new Date(dayHoursMinB[0], dateB[1] - 1, dateB[0], hoursMinB[0], hoursMinB[1],0,0);

        } catch(err) {
            var valueA=new Date(cellA.value);
            var valueB=new Date(cellB.value);

        }

        return valueA-valueB;
     }
 function maf_orderSort(cellA, cellB){
     return cellA.order - cellB.order;
 }
 
//-----------------------------------
//Table filtering library
//-----------------------------------

/*
* Sort a table column
*/
function maf_filter_sort_url(columnId, filterConfig, callbackRefreshTable){
    if(filterConfig){
        var sortStatus=filterConfig.userColumnConfiguration[columnId].sortType;
        if(!sortStatus || sortStatus=="NONE"){
            return;
        }
        //If the column is clicked and no sort orer is defined choose DESC
        if(sortStatus=="UNSORTED"){
            filterConfig.userColumnConfiguration[columnId].sortType="DESC";
            sortStatus="DESC";
        }
        
        var jColumn=$(column);
        var iconHolder=jColumn.find('i[class*="fa"]');
        
        //First sort
        if(iconHolder.attr('class')=='fa fa-folder'){
            //Initialize the sort with the default value
            if(sortStatus=="DESC"){
                iconHolder.removeClass('fa-sort').addClass('fa-chevron-up');
            }else{
                iconHolder.removeClass('fa-sort').addClass('fa-chevron-down');
            }
        }
        
        //Change the filter status for the column
        var ascending=(iconHolder.attr('class')=='fa fa-chevron-down');
        //Switch the icon and update the sortStatus
        if(ascending){
            filterConfig.userColumnConfiguration[columnId].sortType="ASC";
            iconHolder.removeClass('fa-chevron-down').addClass('fa-chevron-up');
        }else{
            filterConfig.userColumnConfiguration[columnId].sortType="DESC";
            iconHolder.removeClass('fa-chevron-up').addClass('fa-chevron-down');
        }        
        callbackRefreshTable.apply();
    }else{
        return;
    }
}


/**
* Update the filter container
*/
function maf_filter_updateFilterContainer(filterContainerId, filterSelectorId, filterConfig, callbackRefreshTable, forceRemove){
    var filterComponentPrefix="_filter_cp_";
    //Remove the filterContainer once updated
    $('#'+filterContainerId+' li').each(function(index){
        var columnId=$(this).attr('id');
        columnId=columnId.substring(filterComponentPrefix.length);
        if(forceRemove || !filterConfig.userColumnConfiguration[columnId].isFiltered){
            $(this).remove();
        }
    });
    //Add new filter components
    for(columnId in filterConfig.userColumnConfiguration){
        var alreadyExists=$('#'+filterContainerId+' li#'+filterComponentPrefix+columnId).length;
        if(filterConfig.userColumnConfiguration[columnId].isFiltered && !alreadyExists){
            var fieldMetaData=filterConfig.selectableColumns[columnId];
            if(fieldMetaData.type=="CHECKBOX"){
                var fieldConfig={
                    "label" : fieldMetaData.name,
                    "defaultValue" : filterConfig.userColumnConfiguration[columnId].filterValue
                };
                maf_filter_addCheckboxField(filterContainerId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable)
            }
            if(fieldMetaData.type=="TEXTFIELD"){
                var fieldConfig={
                        "label" : fieldMetaData.name,
                        "defaultValue" : filterConfig.userColumnConfiguration[columnId].filterValue
                };
                maf_filter_addTextField(filterContainerId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable);
            }
            if(fieldMetaData.type=="NUMERIC"){
                var fieldConfig={
                        "label" : fieldMetaData.name,
                        "defaultValue" : filterConfig.userColumnConfiguration[columnId].filterValue.value,
                        "defaultComparator" : filterConfig.userColumnConfiguration[columnId].filterValue.comparator
                };
                maf_filter_addNumericField(filterContainerId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable);
            }
            if(fieldMetaData.type=="DATERANGE"){
                var fieldConfig={
                    "label" : fieldMetaData.name,
                    "fromDefaultValue" : new Date(filterConfig.userColumnConfiguration[columnId].filterValue.from),
                    "toDefaultValue" : new Date(filterConfig.userColumnConfiguration[columnId].filterValue.to),
                    "format" : fieldMetaData.format
                    };
                maf_filter_addDateRangeField(filterContainerId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable);
            }
            if(fieldMetaData.type=="SELECT"){
                var fieldConfig={
                    "label" : fieldMetaData.name,
                    "defaultValue" : filterConfig.userColumnConfiguration[columnId].filterValue,
                    "values" : fieldMetaData.values};
                maf_filter_addSelect(filterContainerId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable);
            }
            if(fieldMetaData.type=="AUTOCOMPLETE"){
                var fieldConfig={
                        "label" : fieldMetaData.name,
                        "url" : fieldMetaData.url,
                        "defaultValue" : filterConfig.userColumnConfiguration[columnId].filterValue};
                maf_filter_addAutocomplete(filterContainerId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable);
            }
        }
    }
}

/**
 * Prepare the sortable columns
 * @param ajaxContentId
 * @param tableId
 * @param filterConfig
 * @param callbackRefreshTable
 */
function maf_filter_prepareSortableColumns(ajaxContentId, tableId, filterConfig, callbackRefreshTable){
    for(columnId in filterConfig.userColumnConfiguration){
        if(filterConfig.userColumnConfiguration[columnId].isDisplayed &&
                filterConfig.userColumnConfiguration[columnId].sortType!="NONE"){
            var column=$("#"+ajaxContentId+" th#_"+tableId+"_"+columnId);
            var columnHeaderLabel=column.html();
            column.html('');
            var link=$('<a/>',{"href" : "#"});
            link.html(columnHeaderLabel+'&nbsp;<i class="fa"></i>');
            column.append(link);
            
            //Set the initial sort order
            var icon=$("#"+ajaxContentId+" th#_"+tableId+"_"+columnId+" a i");
            maf_filter_setSortOrder(icon, columnId, filterConfig);
            
            link.click(function(ajaxContentId, tableId, columnId, filterConfig, callbackRefreshTable){

                
                return function(event){
        
                    event.preventDefault(); 
                    
                    if(filterConfig.userColumnConfiguration[columnId].sortType=="DESC"){
                        filterConfig.userColumnConfiguration[columnId].sortType="ASC";
                    }else{
                        if(filterConfig.userColumnConfiguration[columnId].sortType=="ASC"){
                            filterConfig.userColumnConfiguration[columnId].sortType="DESC";
                        }else{
                            if(filterConfig.userColumnConfiguration[columnId].sortType=="UNSORTED"){
                                filterConfig.userColumnConfiguration[columnId].sortType="ASC";
                            }
                        }
                    }
                    var icon=$("#"+ajaxContentId+" th#_"+tableId+"_"+columnId+" a i");
                    maf_filter_setSortOrder(icon, columnId, filterConfig);
                    
                    //Un-activate the sort for all the other columns
                    for(otherColumnId in filterConfig.userColumnConfiguration){
                        if(otherColumnId!=columnId){
                            if(filterConfig.userColumnConfiguration[otherColumnId].sortType!="NONE"){
                                filterConfig.userColumnConfiguration[otherColumnId].sortType="UNSORTED";
                                var icon=$("#"+ajaxContentId+" th#_"+tableId+"_"+otherColumnId+" a i");
                                maf_filter_setSortOrder(icon, otherColumnId, filterConfig);
                            }
                        }
                    }
                    
                    callbackRefreshTable.apply();
                }
            }(ajaxContentId, tableId, columnId, filterConfig, callbackRefreshTable));
        }
    }
}

/**
 * Define the display for the specified column
 * @param icon the icon of the column
 * @param columnId the column id
 * @param filterConfig the filter configuration
 */
function maf_filter_setSortOrder(icon, columnId, filterConfig){
    if(filterConfig.userColumnConfiguration[columnId].sortType=="UNSORTED"){
        icon.removeClass('fa-chevron-up');
        icon.removeClass('fa-chevron-down');
    }
    if(filterConfig.userColumnConfiguration[columnId].sortType=="DESC"){
        icon.removeClass('fa-chevron-up');
        if(!icon.hasClass('fa-chevron-down')){
            icon.addClass('fa-chevron-down');
        }
    }
    if(filterConfig.userColumnConfiguration[columnId].sortType=="ASC"){
        icon.removeClass('fa-chevron-down');
        if(!icon.hasClass('fa-chevron-up')){
            icon.addClass('fa-chevron-up');
        }
    }
}

/**
* Prepare the column selector for a display
*/
function maf_filter_prepareColumnsSelector(filterContainerId, columnSelectorId, filterSelectorId, filterConfig, callbackRefreshTable){
    var result=getSourceAndValueForDisplayedColumns(filterConfig);
    $('#'+columnSelectorId).editable({
        title: _maf_translations.columnsSelectorTitle,
        type: 'checklist',
        placement: 'bottom',
        showbuttons: 'bottom',
        onblur: 'submit',
        display: false,
        value: result.value,
        source: result.source,
        success: function(response, newValue) {
            var count=0;
            for(columnId in filterConfig.userColumnConfiguration){
                var index=jQuery.inArray(count+"", newValue);
                if(index!=-1){
                    filterConfig.userColumnConfiguration[columnId].isDisplayed=true;
                }else{
                    filterConfig.userColumnConfiguration[columnId].isDisplayed=false;
                }
                count++;
            }
            var result=getSourceAndValueForFilteredColumns(filterConfig);
            if(result.source.length){
                $('#'+filterSelectorId).show();
                $('#'+filterSelectorId).editable('enable');
                $('#'+filterSelectorId).editable('option', 'source', result.source);
                $('#'+filterSelectorId).editable('setValue', result.value);
            }else{
                $('#'+filterSelectorId).editable('disable');
                $('#'+filterSelectorId).hide();
            }
            maf_filter_updateFilterContainer(filterContainerId, filterSelectorId, filterConfig, callbackRefreshTable, true);
            callbackRefreshTable.apply();
        }
    });
}

/**
* Prepare the filter selector for a display
*/
function maf_filter_prepareFiltersSelector(filterContainerId, filterSelectorId, filterConfig, callbackRefreshTable){
    var result=getSourceAndValueForFilteredColumns(filterConfig);
    $('#'+filterSelectorId).editable({
        title: _maf_translations.filtersSelectorTitle,
        type: 'checklist',
        placement: 'bottom',
        showbuttons: 'bottom',
        onblur: 'submit',
        display: false,
        value: result.value,
        source: result.source,
        success: function(response, newValue) {
            var count=0;
            for(columnId in filterConfig.userColumnConfiguration){
                if(filterConfig.selectableColumns[columnId].type!="NONE"){
                    var index=jQuery.inArray(count+"", newValue);
                    if(index!=-1){
                        filterConfig.userColumnConfiguration[columnId].isFiltered=true;
                    }else{
                        filterConfig.userColumnConfiguration[columnId].isFiltered=false;
                    }
                    count++;
                }
            }
            filterConfig.currentPage = 0;
            maf_filter_updateFilterContainer(filterContainerId, filterSelectorId, filterConfig, callbackRefreshTable, true);
            callbackRefreshTable.apply();
        }
    });
}

function getSourceAndValueForDisplayedColumns(filterConfig){
    var count=0;
    var valueCount=0;
    var value=[];
    var source=[];
    for(columnId in filterConfig.userColumnConfiguration){
        source[count]={};
        source[count].value=count;
        source[count].isKpi=filterConfig.selectableColumns[columnId].isKpi;

        source[count].text = "";
        if (filterConfig.selectableColumns[columnId].isKpi) {
            source[count].text += _maf_translations.kpi + " - ";
        }
        source[count].text += filterConfig.selectableColumns[columnId].name;
        
        if(filterConfig.userColumnConfiguration[columnId].isDisplayed){
            value[valueCount]=count;
            valueCount++;
        }
        count++;
    }
    source.sort(maf_sort_result_source);
    var result={};
    result.value=value;
    result.source=source;
    return result;
}

function getSourceAndValueForFilteredColumns(filterConfig){
    var count=0;
    var valueCount=0;
    var value=[];
    var source=[];
    for(columnId in filterConfig.userColumnConfiguration){
        if(filterConfig.selectableColumns[columnId].type!="NONE"){
            source[count]={};
            source[count].value=count;
            source[count].isKpi=filterConfig.selectableColumns[columnId].isKpi;
            
            source[count].text = "";
            if (filterConfig.selectableColumns[columnId].isKpi) {
                source[count].text += _maf_translations.kpi + " - ";
            }
            source[count].text += filterConfig.selectableColumns[columnId].name;

            if(filterConfig.userColumnConfiguration[columnId].isFiltered){
                value[valueCount]=count;
                valueCount++;
            }
            count++;
        }
    }
    source.sort(maf_sort_result_source);
    var result={};
    result.value=value;
    result.source=source;
    return result;
}

function maf_sort_result_source(a, b) {
    if (a.isKpi == true && b.isKpi == false) {
        return 1;
    }
    if (a.isKpi == false && b.isKpi == true) {
        return -1;
    }
    if ( a.text < b.text )
        return -1;
    if ( a.text > b.text )
        return 1;
    return 0;
}

/**
* Checkbox field configuration
* {
*      "label" : 'a label',
*        "defaultValue" : true
* }
*/
function maf_filter_addCheckboxField(parentId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable){
    var li = maf_filter_construct_field_header(columnId, fieldConfig);
    li.append('<br/>');
    var field = $('<a/>', {"href" : '#',"id" : columnId});  
    li.append(field);   
    $('#'+parentId).append(li);
    var value=[];
    if(fieldConfig.defaultValue){
        value[0]=1;
    }
    $('#' + parentId).find('#'+columnId).editable({
        title : fieldConfig.label,
        type : 'checklist',
        emptytext : _maf_translations.unchecked,
        value: value,
        source : [
            {value: 1, text: _maf_translations.checked}
        ],
        success: function(response, newValue) {
            filterConfig.userColumnConfiguration[columnId].filterValue=(newValue.length!=0);
            filterConfig.currentPage = 0;
            callbackRefreshTable.apply();
        }
    });
    maf_filter_add_remove_event(parentId, columnId, filterSelectorId, filterConfig, callbackRefreshTable);
}

/**
* Text field configuration
* {
*      "label" : 'a label',
*        "defaultValue" : 'a value'
* }
*/
function maf_filter_addTextField(parentId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable){
    var li = maf_filter_construct_field_header(columnId, fieldConfig);
    li.append('<br/>');
    var field = $('<a/>', {"href" : '#',"id" : columnId});  
    li.append(field);   
    $('#'+parentId).append(li);
    $('#' + parentId).find('#'+columnId).editable({
        title : fieldConfig.label,
        emptytext : _maf_translations.empty,
        type : 'text',
        value: fieldConfig.defaultValue,
        success: function(response, newValue) {
            filterConfig.userColumnConfiguration[columnId].filterValue=newValue;
            filterConfig.currentPage = 0;
            callbackRefreshTable.apply();
        }
    });
    maf_filter_add_remove_event(parentId, columnId, filterSelectorId, filterConfig, callbackRefreshTable);
}

/**
* Numeric field configuration
*/
function maf_filter_addNumericField(parentId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable){
    
    var li = maf_filter_construct_field_header(columnId, fieldConfig);
    li.append('<br/>');
    var fieldComparator = $('<a/>', {"href" : '#',"id" : columnId + "Comparator"});
    li.append(fieldComparator);
    li.append('&nbsp;&nbsp;');
    var field = $('<a/>', {"href" : '#',"id" : columnId});
    li.append(field);
    $('#'+parentId).append(li);
    
    $('#' + parentId).find('#'+columnId).editable({
        title : fieldConfig.label,
        emptytext : _maf_translations.empty,
        type : 'text',
        value: fieldConfig.defaultValue,
        success: function(response, newValue) {
            if(!filterConfig.userColumnConfiguration[columnId].filterValue){
                filterConfig.userColumnConfiguration[columnId].filterValue={};
            }
            filterConfig.currentPage = 0;
            filterConfig.userColumnConfiguration[columnId].filterValue.value=newValue;
            callbackRefreshTable.apply();
        }
    });
    
    var source=["=" , "<>", ">", ">=", "<", "<="];
    $('#' + parentId).find('#'+columnId + "Comparator").editable({
        title : fieldConfig.label,
        type : 'select',
        value: fieldConfig.defaultComparator,
        source: source,
        success: function(response, newValue) {
            if(!filterConfig.userColumnConfiguration[columnId].filterValue){
                filterConfig.userColumnConfiguration[columnId].filterValue={};
            }
            filterConfig.currentPage = 0;
            filterConfig.userColumnConfiguration[columnId].filterValue.comparator=newValue;
            callbackRefreshTable.apply();
        }
    });
    
    maf_filter_add_remove_event(parentId, columnId, filterSelectorId, filterConfig, callbackRefreshTable);
}

/**
* Date field configuration
* {
*      "label" : 'a label',
*        "fromDefaultValue" : 'the lower bound',
*        "toDefaultValue" : 'the upper bound',
*      "format" : 'a date format'
* }
*/
function maf_filter_addDateRangeField(parentId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable){

    var li = maf_filter_construct_field_header(columnId, fieldConfig);
    li.append('<br/>' + _maf_translations.from + '&nbsp;');
    var fieldFrom = $('<a/>', {"href" : '#',"id" : columnId+"From"});  
    li.append(fieldFrom);
    li.append("&nbsp;" + _maf_translations.to + "&nbsp;");
    var fieldTo = $('<a/>', {"href" : '#',"id" : columnId+"To"});  
    li.append(fieldTo);
    $('#'+parentId).append(li);
    $('#' + parentId).find('#'+columnId+"From").editable({
        placement: 'bottom',
        type: 'date',
        clear: false,
        datepicker: {language: _maf_language},
        title: _maf_translations.from,
        value: fieldConfig.fromDefaultValue,
        format: fieldConfig.format,
        success: function(response, newValue) {
            if(!filterConfig.userColumnConfiguration[columnId].filterValue){
                filterConfig.userColumnConfiguration[columnId].filterValue={};
            }
            filterConfig.currentPage = 0;
            filterConfig.userColumnConfiguration[columnId].filterValue.from=newValue;
            callbackRefreshTable.apply();
        }
    });
    $('#' + parentId).find('#'+columnId+"To").editable({
        placement: 'bottom',
        type: 'date',
        clear: false,
        datepicker: {language: _maf_language},
        title: _maf_translations.to,
        value: fieldConfig.toDefaultValue,
        format: fieldConfig.format,
        success: function(response, newValue) {
            if(!filterConfig.userColumnConfiguration[columnId].filterValue){
                filterConfig.userColumnConfiguration[columnId].filterValue={};
            }
            filterConfig.currentPage = 0;
            filterConfig.userColumnConfiguration[columnId].filterValue.to=newValue;
            callbackRefreshTable.apply();
        }
    });
    maf_filter_add_remove_event(parentId, columnId, filterSelectorId, filterConfig, callbackRefreshTable);
}

/**
* Select field configuration
* {    
*      "label" : 'a label',
*        "defaultValue" : 'a default value',
*      "values" : {
*             "name1" : {"name" : "name1", "value" : "value1"},
*             "name2" : {"name" : "name2", "value" : "value2"},        
*        }
* }
*/
function maf_filter_addSelect(parentId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable){
    
    var li = maf_filter_construct_field_header(columnId, fieldConfig);
    li.append('<br/>');
    var field = $('<a/>', {"href" : '#',"id" : columnId});  
    li.append(field);
    $('#'+parentId).append(li);
    
    var source = [];
    var values = [];
    var valuesIndex = 0;
    var count = 0;

    for(key in fieldConfig.values){
        source[count]={};
        source[count].value=fieldConfig.values[key].value;
        source[count].text=fieldConfig.values[key].name;
        source[count].order=fieldConfig.values[key].order;
        if (fieldConfig.defaultValue.indexOf(fieldConfig.values[key].value) >= 0) {
            values[valuesIndex] = fieldConfig.values[key].value;
            valuesIndex++;
        }
        count++;
    }
    
    source.sort(maf_orderSort);

    $('#' + parentId).find('#'+columnId).editable({
        title: fieldConfig.label,
        type: 'checklist',
        placement: 'bottom',
        value: values,
        source: source,
        success: function(response, newValue) {
            filterConfig.userColumnConfiguration[columnId].filterValue=newValue;
            filterConfig.currentPage = 0;
            callbackRefreshTable.apply();
        }
    });

    maf_filter_add_remove_event(parentId, columnId, filterSelectorId, filterConfig, callbackRefreshTable);
}

/**
* Autocomplete field configuration
* {    
*      "label" : 'a label',
*      "url" : 'http://url/'
* }
*/
function maf_filter_addAutocomplete(parentId, columnId, filterSelectorId, fieldConfig, filterConfig, callbackRefreshTable){
    var li = maf_filter_construct_field_header(columnId, fieldConfig);
    li.append('<div class="clearfix"></div>');
    var field = $('<a/>', {"href" : '#',"id" : "_"+columnId+"_editable"});
    li.append(field);
    var hiddenFieldValue=$('<input/>', {"type" : "hidden", "name" : columnId, "id" : columnId, "value" : ""});
    var hiddenFieldContent=$('<input/>', {"type" : "hidden", "name" : columnId + "_content", "id" : columnId  + "_content", "value" : ""});  
    li.append(hiddenFieldValue);
    li.append(hiddenFieldContent);
    
    var clear = $('<a/>', {"href" : '#',"id" : '_'+columnId+'_clear_editable', 'class' : 'btn btn-default btn-sm'});
    clear.append($('<span/>', {"class" : "fa fa-eraser"}));
    li.append(" &nbsp;")
    li.append(clear);

    $('#'+parentId).append(li);

    $('#' + parentId).find("#_"+columnId+"_clear_editable").click(function(e) {
        e.preventDefault();
        $('#' + parentId).find('#_'+columnId+'_editable').editable('setValue', null);
        $('#' + parentId).find('#'+columnId).val(null);
        $('#' + parentId).find('#'+columnId+"_content").val("");
        filterConfig.userColumnConfiguration[columnId].filterValue={"value" : null, "content" : ""};
        filterConfig.currentPage = 0;
        callbackRefreshTable.apply();
    });
    
    window[columnId+"_editableCache"]={};
    if (fieldConfig.defaultValue.content != "") {
        window[columnId+"_editableCache"][fieldConfig.defaultValue.value] = {'name' : fieldConfig.defaultValue.content};
    }
    maf_activateEditable_for_autocomplete(
            window[columnId+"_editableCache"],
            parentId,
            columnId, 
            fieldConfig.defaultValue.value, 
            "", 
            fieldConfig.url,
            "",
            function(newValue, newValueContent){
                filterConfig.userColumnConfiguration[columnId].filterValue={"value" : newValue, "content" : newValueContent};
                filterConfig.currentPage = 0;
                callbackRefreshTable.apply();
            });
    maf_filter_add_remove_event(parentId, columnId, filterSelectorId, filterConfig, callbackRefreshTable);
}

function maf_filter_construct_field_header(columnId, fieldConfig) {
    var li = $('<li/>', {"class" : 'filter-input', "id" : "_filter_cp_"+columnId});
    var leftDiv = $('<div/>', {"class" : "pull-left"});
    var rightDiv = $('<div/>', {"class" : "pull-right"});
    var removeA = $('<a/>', {"href" : "#", "id" : "_filter_remove_" + columnId, "html" : "<span class='fa fa-times'></span>"});
    var label = $('<strong/>',{text : fieldConfig.label}); 
    leftDiv.append(label);
    rightDiv.append("&nbsp;&nbsp;");
    rightDiv.append(removeA);
    li.append(leftDiv);
    li.append(rightDiv);
    return li;
}

/**
 * create the remove event for removing a filter
 */
function maf_filter_add_remove_event(parentId, columnId, filterSelectorId, filterConfig, callbackRefreshTable) {

    $('#' + parentId).find('#_filter_remove_'+columnId).click(function(event) {

        event.preventDefault(); 

        //set filter as disable
        filterConfig.userColumnConfiguration[columnId].isFiltered=false;

        //update the checkbox list
        var result=getSourceAndValueForFilteredColumns(filterConfig);
        $('#' + filterSelectorId).editable('setValue', result.value);

        //update the filters container
        filterConfig.currentPage = 0;
        maf_filter_updateFilterContainer(parentId, filterSelectorId, filterConfig, callbackRefreshTable, false);

        //update the table
        callbackRefreshTable.apply();
    });
}

/**
 * A method to activate the X-editable code for an autocomplete
 * @param editableCache an associative array which contains a set of key value like used to
 * cache the values retreived from the ajax url
 * {
*             "name1" : {"name" : "name1", "value" : "value1"},
*             "name2" : {"name" : "name2", "value" : "value2"},        
*  }
 * @param fieldId the id of the field
 * @param fieldValue the value of the field
 * @param fieldLabel the label of the field
 * @param url an URL (see autocomplete.scala.html for the parameters)
 * @param contextQueryString a context query string (see autocomplete.scala.html)
 */
function maf_activateEditable_for_autocomplete(editableCache,container,fieldId, fieldValue, fieldLabel, url,contextQueryString, callbackMethod){
	
	var jContainer;
	if (container == "") {
		jContainer = $('html');
	} else {
		jContainer = $('#' + container);
	}

    if(editableCache[fieldValue]){
    	jContainer.find('#'+fieldId).val(fieldValue);
    	jContainer.find('#'+fieldId+"_content").val(editableCache[fieldValue].name);
    }

    jContainer.find('#_'+fieldId+'_editable').editable({
        title: 'fieldLabel',
        mode: 'inline',
        type: 'typeaheadjs',
        onblur: 'submit',
        value: jContainer.find('#'+fieldId+"_content").val(),
        valueKey: 'name',
        typeahead: {
             remote: {
                 url: url+'?query=%QUERY'+contextQueryString,
                 filter: function(parsedResponse){
                    var result=[];
                    for(value in parsedResponse){
                        
                        editableCache[value]=parsedResponse[value];
                        result.push(parsedResponse[value].name);
                    }
                    return result;
                 }
             }
        },
        validate: function(newValue) {
            var isValid=false;
             for(value in editableCache){
                 if(editableCache[value].name==newValue){
                     isValid=true;
                 }
             }
             if(!isValid){
                 return {newValue: jContainer.find('#'+fieldId+"_content").val()};
             }
        },
         success: function(response, newValue) {
             for(value in editableCache){
                 if(editableCache[value].name==newValue){
                     jContainer.find('#'+fieldId).val(value).trigger('change');
                     jContainer.find('#'+fieldId+"_content").val(newValue);
                     if(callbackMethod){
                         callbackMethod.apply(this, new Array(value, newValue));
                     }
                     return;
                 }
             }
        }
    });
}
