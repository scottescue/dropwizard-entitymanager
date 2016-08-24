require 'nokogiri'

xpath_query = ARGV[0]
if xpath_query.nil?
  abort 'No XPath query provided'
end

doc = File.open("pom.xml") { |f| Nokogiri::XML(f) }.remove_namespaces!
value = doc.xpath(xpath_query)

if value.nil? or value.empty?
  abort 'No match for the XPath query'
end

if value.count > 1
  abort 'More than one match for the XPath query'
end

puts value.text

