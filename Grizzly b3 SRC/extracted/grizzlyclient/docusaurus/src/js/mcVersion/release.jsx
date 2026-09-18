import { usePluginData } from '@docusaurus/useGlobalData'

export default function ReleaseVersion() {
  const { release } = usePluginData('mc-version')
  return <span>{release}</span>
}
