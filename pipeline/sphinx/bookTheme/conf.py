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
import os
import sys
sys.path.insert(0, os.path.abspath('../../../generated'))
# _currentModelProperties.py in folder generated
import _currentModelProperties


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
    'sphinxcontrib.jquery', # This way jquery is loaded before custom.js
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

# see: https://github.com/sphinx-contrib/plantuml
plantuml = 'java -jar ' + os.environ["plantuml_jar"]
#plantuml = 'java -jar /home/wackerow/software/plantuml/plantuml-1.2025.2.jar'
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

#html_theme = 'classic'
html_theme = 'sphinx_book_theme'
#html_theme = 'pydata_sphinx_theme'
html_theme_options = {
#    toggle-primary-sidebar found by debugging _templates/sections/header-article.html, <h3>{{ item }}</h3
#    Other solution possible by custom _templates/sections/header-article.html.
    'article_header_start'  : ['toggle-primary-sidebar', 'breadcrumbs'],
    #'back_to_top_button'    : False, # WARNING: unsupported theme option 'back_to_top_button' given
    # 'external_links': [
    #     {'name': 'require', 'url': 'https://requirejs.org/docs/start.html'}
    # ],
    'logo'                  : {
        'link': _currentModelProperties.model_uri,
        'text': _currentModelProperties.model_title
    },
    'navigation_with_keys'  : True,
#    'primary_sidebar_end'   : ['indices.html'],
    'pygment_light_style'   : 'xcode', # see: https://pygments.org/styles/
    #'repository_url'        : _currentModelProperties.repository_url,
    #'search_as_you_type'    : True,  # WARNING: unsupported theme option
    'secondary_sidebar_items': [],      # pydata: disable secondary sidebar
    # same as above for a specific page only ':html_theme.sidebar_secondary.remove: True' in rst file
    #'toc_title'             : 'UML Model Contents', # only right sidebar, see https://sphinx-book-theme.readthedocs.io/en/stable/sections/sidebar-secondary.html#rename-the-in-page-table-of-contents
    'use_download_button'   : False,
    'use_fullscreen_button' : True,
    'use_issues_button'     : False,
    'use_repository_button' : False
}
# used by template model-title
html_context = {
    'model_acronym'         : _currentModelProperties.model_acronym,
    'model_major_version'   : _currentModelProperties.model_major_version,
    'model_minor_version'   : _currentModelProperties.model_minor_version
}

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named 'default.css' will overwrite the builtin 'default.css'.
html_static_path = ['_static']
html_css_files = [
	'custom.css',
    'diagramModal.css'
]
html_js_files = [
	'custom.js',
    '_modelInformation.js'
]

html_show_sphinx     = False
html_show_sourcelink = False
#home_page_in_toc = True ??

html_sidebars = {
    '**': ['navbar-logo', 'model-title', 'search-field', 'globaltoc.html', 'indices']
}

language = "en"
