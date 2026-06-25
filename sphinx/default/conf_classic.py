# conf_classic.py. To use it: copy to conf.py.

# Configuration file for the Sphinx documentation builder.
#
# This file only contains a selection of the most common options. For a full
# list see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Path setup --------------------------------------------------------------

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
# import os
# import sys
# sys.path.insert(0, os.path.abspath('.'))


# -- Project information -----------------------------------------------------

project = ''
copyright = ''
author = ''


# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = [
    # 'hoverxref.extension',
	'sphinx.ext.graphviz',
    'sphinx_copybutton',
    'sphinx_design', # also used by pydata which is used by book theme
#	'sphinxcontrib.contentui',
#	'sphinxcontrib.excel', # geht nicht
#	'sphinxcontrib.exceltable', geht nicht
	'sphinxcontrib.plantuml',
#	'sphinx_panels',
#	'sphinx_rtd_theme',
#	'sphinx_tabs.tabs',
#	'sphinx_togglebutton'
]

# hoverxref_roles = [
#     'term'
# ]
# hoverxref not working with local files.
# See https://sphinx-hoverxref.readthedocs.io/en/latest/development.html#avoid-cors-on-local-backend.

plantuml = 'java -jar C:/Programs/PlantUML/plantuml-1.2023.12.jar'
#plantuml = 'java -jar C:/Programs/PlantUML/plantuml-1.2022.6.jar'
#plantuml = 'java -jar C:/Programs/PlantUML/plantuml-1.2022.2.jar'
plantuml_output_format = 'svg_obj' # svg only
#plantuml_output_format = 'svg'    # svg + png

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This pattern also affects html_static_path and html_extra_path.
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']


# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
#
#html_theme = 'sphinx_rtd_theme'
#html_theme = 'default'

html_theme = 'classic'
#html_theme = 'sphinx_book_theme'
#html_theme = 'pydata_sphinx_theme'
html_theme_options = {
 	'body_max_width'		: 'auto',
 	'collapsiblesidebar'	: 'true',	# classic: Add an experimental JavaScript snippet that makes the sidebar collapsible via a button on its side
 	'externalrefs'			: 'true',	# classic: Display external links differently from internal links
 	'navigation_with_keys'	: 'true',	# Allow navigating to the previous/next page using the keyboard’s left and right arrows
 	'sidebarwidth'			: '290px',	# classic: width for the widest TOC entry, also width of the logo
 	'stickysidebar'			: 'false',	# classic: not possible together with collapsiblesidebar
#    'secondary_sidebar_items': []      # pydata: disable secondary sidebar
}

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named 'default.css' will overwrite the builtin 'default.css'.
html_static_path = ['_static']
html_css_files = [
	'custom.css' # classic theme
]
html_js_files = [
	'custom.js',
    'definition.js'
]

html_show_sphinx = False
html_show_sourcelink = False

html_sidebars = {
	'**': ['searchbox.html', 'globaltoc.html', 'sourcelink.html']
}
