def assert(expression, message)
  abort "Skipping deployment: #{message}" unless expression
end

def valid_tag?(tag)
  tag =~ /^v[0-9]\.[0-9]\.[0-9]\-[0-9]$/
end

def valid_version?(version)
  version =~ /^[0-9]\.[0-9]\.[0-9]\-[0-9]$/
end


git_tag = ENV['TRAVIS_TAG']
pom_version = ENV['POM_VERSION']
pom_scm_tag = ENV['POM_SCM_TAG']


# Ensure we're building a valid release tag
assert(!git_tag.nil?, 'Not building a tag')
assert(valid_tag?(git_tag), "Tag '#{git_tag}' is not a properly formatted release tag")

# Ensure the project has a valid release version configured in the POM
assert(!pom_version.nil?, 'Artifact version is not specified in the POM')
assert(valid_version?(pom_version), "#{pom_version} is not a valid release version")

# Ensure the project has a tag configured for SCM in the POM
assert(!pom_scm_tag.nil?, 'SCM tag is not specified in the POM')

# Ensure the Git tag, project version, and SCM tag are all in agreement
assert(pom_scm_tag == git_tag, "SCM tag '#{pom_scm_tag}' in the POM does not match the '#{git_tag}' tag being built")
assert(pom_scm_tag == "v#{pom_version}", "SCM tag '#{pom_scm_tag}' in the POM does not match version '#{pom_version}' in the POM")