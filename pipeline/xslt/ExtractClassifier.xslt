<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:StandardProfile="http://www.omg.org/spec/UML/20131001/StandardProfile"
	xmlns:uml="http://www.omg.org/spec/UML/20131001"
	xmlns:xmi="http://www.omg.org/spec/XMI/20131001"
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:f="urn:extract"
	exclude-result-prefixes="StandardProfile uml xmi xs f">
	
	<!--
						ExtractClassifier.xsl
						
						Purpose
						Extract UML classifiers from a selected subtree of a UML XMI document
						and write each classifier into its own output file.
						
						Supported classifier kinds
						- uml:Class
						- uml:DataType
						- uml:Enumeration
						- uml:PrimitiveType
						
						Root selection strategy
						1. Prefer MAIN_ROOT_ID when supplied.
						2. Otherwise, if MAIN_PACKAGE_NAME is supplied, prefer a matching uml:Model.
						3. Only if no matching uml:Model exists, try a matching uml:Package.
						4. If multiple candidates exist within the chosen category, terminate with
						diagnostics and require MAIN_ROOT_ID.
						
						Path behavior
						- Output file paths are computed relative to the exact selected root node.
						- The path may include both uml:Model and uml:Package hierarchy levels.
						- The selected root may be included or excluded from the output path.
						- Each classifier name and path segment is sanitized for filesystem use.
						
						Intended processor
						- Saxon with XSLT 2.0 support.
						
						Important behavior
						- Each matched classifier is copied as-is using xsl:copy-of.
						- The stylesheet writes one file per classifier using xsl:result-document.
						- If a fully standalone XMI wrapper is required, the classifier template
						would need to be extended accordingly.
	-->
	
	<!-- Serialize output documents as indented XML -->
	<xsl:output method="xml" encoding="UTF-8" indent="yes"/>
	
	<!--
						MAIN_ROOT_ID
						Preferred root selector.
						
						Expected value
						- @xmi:id of a uml:Model
						- or @xmi:id of a packagedElement with @xmi:type='uml:Package'
						
						If present and uniquely resolved, this parameter takes precedence over
						MAIN_PACKAGE_NAME.
	-->
	<xsl:param name="MAIN_ROOT_ID" as="xs:string?" select="()"/>
	
	<!--
						MAIN_PACKAGE_NAME
						Fallback selector by logical name.
						
						Matching order
						- first: uml:Model[name = MAIN_PACKAGE_NAME]
						- second: packagedElement[@xmi:type='uml:Package'][name = MAIN_PACKAGE_NAME]
						
						This implements the rule: always prefer uml:Model over uml:Package.
	-->
	<xsl:param name="MAIN_PACKAGE_NAME" as="xs:string?" select="()"/>
	
	<!--
						INCLUDE_ROOT_IN_PATH
						Controls whether the selected root contributes its own path segment.
						
						false() means path starts below the selected root.
						true() means the selected root name becomes the first path segment.
	-->
	<xsl:param name="INCLUDE_ROOT_IN_PATH" as="xs:boolean" select="false()"/>
	
	<!--
						f:safe-name
						Convert a UML name into a filesystem-safe path segment or file name.
	-->
	<xsl:function name="f:safe-name" as="xs:string">
		<xsl:param name="raw" as="xs:string?"/>
		
		<xsl:variable name="trimmed" select="normalize-space($raw)"/>
		<xsl:variable name="replaced" select="replace($trimmed, '[\\/:*?&quot;&lt;&gt;|]', '_')"/>
		<xsl:variable name="collapsed" select="replace($replaced, '\s+', '_')"/>
		
		<xsl:sequence select="if ($collapsed != '') then $collapsed else '_unnamed_'"/>
	</xsl:function>
	
	<!--
						f:is-container
						Return true when the node is relevant as a hierarchy container for
						output path construction.
						
						Supported container types
						- uml:Model
						- packagedElement with @xmi:type='uml:Package'
	-->
	<xsl:function name="f:is-container" as="xs:boolean">
		<xsl:param name="node" as="node()?"/>
		
		<xsl:sequence select="
			exists(
				$node[
					self::uml:Model
					or self::packagedElement[@xmi:type='uml:Package']
				]
			)
				"/>
	</xsl:function>
	
	<!--
						f:describe-node
						Produce a readable description of a candidate root node for diagnostics.
	-->
	<xsl:function name="f:describe-node" as="xs:string">
		<xsl:param name="n" as="element()"/>
		
		<xsl:variable name="kind"
			select="
				if ($n/self::uml:Model) then 'uml:Model'
				else if ($n/self::packagedElement[@xmi:type='uml:Package']) then 'uml:Package'
				else name($n)
				"/>
		
		<xsl:variable name="node-name" select="string($n/name)"/>
		<xsl:variable name="node-id" select="string($n/@xmi:id)"/>
		
		<xsl:sequence
			select="concat($kind, ' name=&quot;', $node-name, '&quot; xmi:id=&quot;', $node-id, '&quot;')"/>
	</xsl:function>
	
	<!--
						f:relative-path
						Build the output directory path for a classifier relative to the exact
						selected root node.
						
						Steps
						- Collect classifier ancestors.
						- Keep only valid container nodes.
						- Keep only containers that are the selected root or are below it.
						- Optionally omit the selected root itself.
						- Convert each container name into a safe path segment.
						- Join the path segments using forward slash.
	-->
	<xsl:function name="f:relative-path" as="xs:string?">
		<xsl:param name="classifier" as="element()"/>
		<xsl:param name="root" as="element()"/>
		<xsl:param name="include-root" as="xs:boolean"/>
		
		<xsl:variable name="containers" as="element()*"
			select="
				$classifier/ancestor::*[
					f:is-container(.)
					and (. is $root or . >> $root)
				]
				"/>
		
		<xsl:variable name="parts" as="xs:string*"
			select="
				for $c in $containers
				return
					if (not($include-root) and $c is $root)
						then ()
					else f:safe-name(string($c/name))
				"/>
		
		<xsl:sequence
			select="if (exists($parts)) then string-join($parts, '/') else ()"/>
	</xsl:function>
	
	<!--
						Document root template.
						
						Root resolution order
						1. Resolve by MAIN_ROOT_ID if supplied.
						2. If no id-based root exists, search uml:Model by MAIN_PACKAGE_NAME.
						3. If no model match exists, search uml:Package by MAIN_PACKAGE_NAME.
						4. If multiple matches occur in the selected category, terminate.
						5. If a unique root is found, extract all classifier descendants below it.
						
						This guarantees that name-based lookup always prefers uml:Model over
						uml:Package.
	-->
	<xsl:template match="/">
		
		<!-- Try exact lookup by xmi:id first -->
		<xsl:variable name="root-by-id" as="element()*"
			select="
				if (exists($MAIN_ROOT_ID) and normalize-space($MAIN_ROOT_ID) != '')
					then (
						//uml:Model[@xmi:id = $MAIN_ROOT_ID],
						//packagedElement[@xmi:type='uml:Package'][@xmi:id = $MAIN_ROOT_ID]
					)
				else ()
				"/>
		
		<!-- Name-based lookup, first stage: prefer matching uml:Model -->
		<xsl:variable name="model-by-name" as="element()*"
			select="
				if (empty($root-by-id)
					and exists($MAIN_PACKAGE_NAME)
					and normalize-space($MAIN_PACKAGE_NAME) != '')
					then //uml:Model[name = $MAIN_PACKAGE_NAME]
				else ()
				"/>
		
		<!-- Name-based lookup, second stage: only if no model matched -->
		<xsl:variable name="package-by-name" as="element()*"
			select="
				if (empty($root-by-id)
					and empty($model-by-name)
					and exists($MAIN_PACKAGE_NAME)
					and normalize-space($MAIN_PACKAGE_NAME) != '')
					then //packagedElement[@xmi:type='uml:Package'][name = $MAIN_PACKAGE_NAME]
				else ()
				"/>
		
		<xsl:choose>
			
			<!-- MAIN_ROOT_ID must resolve to at most one valid root -->
			<xsl:when test="exists($MAIN_ROOT_ID)
				and normalize-space($MAIN_ROOT_ID) != ''
				and count($root-by-id) gt 1">
				<xsl:message terminate="yes">
					MAIN_ROOT_ID '<xsl:value-of select="$MAIN_ROOT_ID"/>' matched multiple roots:
					<xsl:for-each select="$root-by-id">
						<xsl:text>&#10; - </xsl:text>
						<xsl:value-of select="f:describe-node(.)"/>
					</xsl:for-each>
				</xsl:message>
			</xsl:when>
			
			<!-- Multiple models with the same name are still ambiguous -->
			<xsl:when test="empty($root-by-id) and count($model-by-name) gt 1">
				<xsl:message terminate="yes">
					MAIN_PACKAGE_NAME '<xsl:value-of select="$MAIN_PACKAGE_NAME"/>' matched multiple uml:Model roots:
					<xsl:for-each select="$model-by-name">
						<xsl:text>&#10; - </xsl:text>
						<xsl:value-of select="f:describe-node(.)"/>
					</xsl:for-each>
					<xsl:text>&#10;Use MAIN_ROOT_ID instead.</xsl:text>
				</xsl:message>
			</xsl:when>
			
			<!-- Multiple packages with the same name are ambiguous if no model matched -->
			<xsl:when test="empty($root-by-id) and empty($model-by-name) and count($package-by-name) gt 1">
				<xsl:message terminate="yes">
					MAIN_PACKAGE_NAME '<xsl:value-of select="$MAIN_PACKAGE_NAME"/>' matched multiple uml:Package roots:
					<xsl:for-each select="$package-by-name">
						<xsl:text>&#10; - </xsl:text>
						<xsl:value-of select="f:describe-node(.)"/>
					</xsl:for-each>
					<xsl:text>&#10;Use MAIN_ROOT_ID instead.</xsl:text>
				</xsl:message>
			</xsl:when>
			
			<xsl:otherwise>
				<!-- Choose the single effective root according to the priority rules -->
				<xsl:variable name="root" as="element()?"
					select="($root-by-id, $model-by-name, $package-by-name)[1]"/>
				
				<!-- Fail explicitly if no root could be resolved -->
				<xsl:if test="empty($root)">
					<xsl:message terminate="yes">
						No root found.
						MAIN_ROOT_ID='<xsl:value-of select="$MAIN_ROOT_ID"/>'
						MAIN_PACKAGE_NAME='<xsl:value-of select="$MAIN_PACKAGE_NAME"/>'
					</xsl:message>
				</xsl:if>
				
				<!-- Process all supported classifier descendants of the selected root -->
				<xsl:apply-templates
					select="
						$root//packagedElement[
							@xmi:type='uml:Class'
							or @xmi:type='uml:DataType'
							or @xmi:type='uml:Enumeration'
							or @xmi:type='uml:PrimitiveType'
						]
						">
					<xsl:with-param name="root" as="element()" select="$root"/>
				</xsl:apply-templates>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!--
						Classifier extraction template.
						
						For each matched classifier
						- compute a safe file name
						- compute its path relative to the exact selected root
						- write one XMI fragment file
	-->
	<xsl:template match="packagedElement[
			@xmi:type='uml:Class'
			or @xmi:type='uml:DataType'
			or @xmi:type='uml:Enumeration'
			or @xmi:type='uml:PrimitiveType'
		]">
		
		<!-- The exact selected root node is passed in from the document root template -->
		<xsl:param name="root" as="element()"/>
		
		<!-- Safe file name for the classifier -->
		<xsl:variable name="classifier-name" as="xs:string"
			select="f:safe-name(string(name))"/>
		
		<!-- Relative directory path below or including the root -->
		<xsl:variable name="relative-path" as="xs:string?"
			select="f:relative-path(., $root, $INCLUDE_ROOT_IN_PATH)"/>
		
		<!-- Full output path for xsl:result-document -->
		<xsl:variable name="href" as="xs:string"
			select="
				if ($relative-path)
					then concat($relative-path, '/', $classifier-name, '.xmi')
				else concat($classifier-name, '.xmi')
				"/>
		
		<!-- Write the current classifier node as its own output file -->
		<xsl:result-document href="{$href}" method="xml" encoding="UTF-8" indent="yes">
			<xsl:copy-of select="."/>
		</xsl:result-document>
	</xsl:template>
	
</xsl:stylesheet>