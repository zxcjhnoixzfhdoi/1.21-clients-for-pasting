const RELEASE_API = 'https://git.luka.onl/api/v1/repos/luka/GrizzlyClient/releases/latest'
const EXPERIMENTAL_PROPERTIES =
  'https://git.luka.onl/luka/GrizzlyClient/raw/branch/main/gradle.properties'

const releaseProperties = (tag) =>
  `https://git.luka.onl/luka/GrizzlyClient/raw/tag/${tag}/gradle.properties`

const UNKNOWN = 'unknown'

function parseMinecraftVersion(text) {
  const match = text.match(/^minecraft_version=(.+)$/m)
  return match ? match[1].trim() : UNKNOWN
}

async function fetchText(url) {
  const response = await fetch(url)
  if (!response.ok) throw new Error(`${url} responded ${response.status}`)
  return response.text()
}

async function fetchReleaseVersion() {
  const response = await fetch(RELEASE_API)
  if (!response.ok) throw new Error(`${RELEASE_API} responded ${response.status}`)

  const tag = (await response.json()).tag_name
  if (!tag) return UNKNOWN

  return parseMinecraftVersion(await fetchText(releaseProperties(tag)))
}

async function fetchExperimentalVersion() {
  return parseMinecraftVersion(await fetchText(EXPERIMENTAL_PROPERTIES))
}

async function resolve(label, load) {
  try {
    return await load()
  } catch (error) {
    console.warn(`[mc-version] could not resolve the ${label} version: ${error.message}`)
    return UNKNOWN
  }
}

export default function mcVersionPlugin() {
  return {
    name: 'mc-version',

    async loadContent() {
      const [release, experimental] = await Promise.all([
        resolve('release', fetchReleaseVersion),
        resolve('experimental', fetchExperimentalVersion),
      ])
      return { release, experimental }
    },

    contentLoaded({ content, actions }) {
      actions.setGlobalData(content)
    },
  }
}
