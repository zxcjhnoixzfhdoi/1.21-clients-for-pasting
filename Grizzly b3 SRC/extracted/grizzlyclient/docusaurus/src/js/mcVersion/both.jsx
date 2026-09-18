import { usePluginData } from '@docusaurus/useGlobalData'

export default function FabricInfo() {
  const { release, experimental } = usePluginData('mc-version')

  if (release === experimental) {
    return <span>You need Minecraft version {release} to run Grizzly Client.</span>
  }

  return <span>You need Minecraft version {release} to run Grizzly Client.
  <br></br>However, if you use the experimental version of Grizzly, you need {experimental}.</span>
}
