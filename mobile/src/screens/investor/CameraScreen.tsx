import React, { useState } from 'react'
import { View, Text, Image, StyleSheet, Alert, Linking } from 'react-native'
import type { NativeStackScreenProps } from '@react-navigation/native-stack'
import * as ImagePicker from 'expo-image-picker'
import * as ImageManipulator from 'expo-image-manipulator'
import type { InvestorStackParamList } from '../../navigation/types'
import { ApexButton } from '../../components/ApexButton'
import { StepIndicator } from '../../components/StepIndicator'
import { Colors } from '../../theme/colors'

type Props = NativeStackScreenProps<
  InvestorStackParamList,
  'CameraFront' | 'CameraBack'
>

export function CameraScreen({ route, navigation }: Props) {
  const { side, amount, accountId } = route.params
  const isFront = side === 'front'
  const frontBase64 = 'frontBase64' in route.params ? route.params.frontBase64 : undefined

  const [pendingUri, setPendingUri] = useState<string | null>(null)
  const [processing, setProcessing] = useState(false)

  async function handleTakePhoto() {
    const { status } = await ImagePicker.getCameraPermissionsAsync()

    if (status === 'denied') {
      Alert.alert(
        'Camera Access Required',
        'Camera access is required to take photos of your check. Tap below to open Settings.',
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Open Settings', onPress: () => Linking.openSettings() },
        ]
      )
      return
    }

    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      base64: false,
      quality: 0.9,
    })

    if (!result.canceled && result.assets[0]) {
      setPendingUri(result.assets[0].uri)
    }
  }

  async function handleChooseFromLibrary() {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.9,
    })

    if (!result.canceled && result.assets[0]) {
      setPendingUri(result.assets[0].uri)
    }
  }

  async function handleUsePhoto() {
    if (!pendingUri) return
    setProcessing(true)

    try {
      const manipulated = await ImageManipulator.manipulateAsync(
        pendingUri,
        [{ resize: { width: 1200 } }],
        { compress: 0.8, format: ImageManipulator.SaveFormat.JPEG, base64: true }
      )

      const base64 = manipulated.base64!

      if (isFront) {
        navigation.navigate('CameraBack', {
          amount,
          accountId,
          frontBase64: base64,
          side: 'back',
        })
      } else {
        navigation.navigate('Review', {
          amount,
          accountId,
          frontBase64: frontBase64!,
          backBase64: base64,
        })
      }
    } finally {
      setProcessing(false)
    }
  }

  // Confirming state — show preview
  if (pendingUri) {
    return (
      <View style={styles.container}>
        <StepIndicator current={isFront ? 2 : 3} total={4} />
        <Image source={{ uri: pendingUri }} style={styles.preview} resizeMode="contain" />
        <View style={styles.buttons}>
          <ApexButton
            title="Use This Photo"
            onPress={handleUsePhoto}
            loading={processing}
          />
          <ApexButton
            title="Retake"
            variant="secondary"
            onPress={() => setPendingUri(null)}
            style={{ marginTop: 12 }}
          />
        </View>
      </View>
    )
  }

  // Idle state — instructions + capture buttons
  return (
    <View style={styles.container}>
      <View>
        <StepIndicator current={isFront ? 2 : 3} total={4} />
        <Text style={styles.instructions}>
          Take a photo of the {isFront ? 'front' : 'back'} of your check.
        </Text>
        <Text style={styles.tip}>
          Place the check on a dark, flat surface with good lighting.
        </Text>
      </View>
      <View style={styles.buttons}>
        <ApexButton title="Take Photo" onPress={handleTakePhoto} />
        <ApexButton
          title="Choose from Library"
          variant="secondary"
          onPress={handleChooseFromLibrary}
          style={{ marginTop: 12 }}
        />
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
    paddingHorizontal: 24,
    paddingBottom: 40,
    justifyContent: 'space-between',
  },
  instructions: {
    fontSize: 20,
    fontWeight: '600',
    color: Colors.text,
    textAlign: 'center',
    marginTop: 24,
  },
  tip: {
    fontSize: 14,
    color: Colors.textMuted,
    textAlign: 'center',
    marginTop: 8,
  },
  preview: {
    flex: 1,
    marginVertical: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  buttons: {
    width: '100%',
  },
})
