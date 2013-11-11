<!DOCTYPE html>
<html>
<head>
  	<title>Testing Resources plugin</title>
   	<r:require modules="application"/>
    <style>
	  	textarea { }
	  	input[id="asunto"] { }
  	</style>
  	<r:script>
	  	$(document).ready(function(){
	  		$('#testForm').submit(function(event){event.preventDefault();});
	
		  	$("#submitButton").click(function(){});
	  	});
  	</r:script>
	<r:layoutResources />
</head>
<body>
	<form id="testForm" style="display:block;margin:20px auto 30px auto; width:40%;">
		    <label for="one">URL: </label>
		    	<input type="text" id="urlCentroControl">
		    <button id="submitButton">Submit</button>
	</form>
</body>
</html>
<r:layoutResources />