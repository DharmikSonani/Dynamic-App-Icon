import { Text, View } from 'react-native'
import React from 'react'
import { useDefaultIcon, useIconB, useIconC } from './hooks/useAppIcons'

const App = () => {

  useDefaultIcon();
  // useIconB();
  // useIconC();

  return (
    <View style={{
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
    }}>
      <Text>App</Text>
    </View>
  )
}

export default App